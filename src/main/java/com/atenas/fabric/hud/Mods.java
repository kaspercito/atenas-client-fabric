package com.atenas.fabric.hud;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.atenas.fabric.net.Social;
import com.atenas.fabric.net.TeamSync;
import com.atenas.fabric.shared.Config;
import com.atenas.fabric.shared.SharedDir;
import com.atenas.fabric.shared.WaypointStore;
import com.atenas.fabric.voice.VoiceClient;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Modulos con logica por tick: zoom, perspectiva, fullbright, autotext, voz, Team View y el contexto de mundo de los waypoints. */
public final class Mods {
    private Mods() {}

    private static final KeyBinding.Category CAT = KeyBinding.Category.create(Identifier.of("atenasclientfabric", "main"));
    private static KeyBinding menu, zoom, fullbright, perspective, ptt, mute, deafen;
    private static final KeyBinding[] AUTO = new KeyBinding[4];
    private static final String[] AUTO_TEXT = new String[4];

    private static boolean autoSprint;
    private static boolean fbOn;

    // zoom: multiplicador actual (1 = sin zoom). Lo lee ZoomFovMixin; si ese mixin no se aplico (fovHooked) se usa el FOV de las opciones.
    public static boolean fovHooked;
    private static float zoomCur = 1f;
    private static boolean legacyFov; private static int savedFov;

    /** Tope del historial del chat con "Chat largo" (lineas y mensajes). Vanilla: 100. */
    public static final int CHAT_MAX = 20000;
    private static Text lastStamped;

    private static KeyBinding key(String id, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.atenasclientfabric." + id, InputUtil.Type.KEYSYM, code, CAT));
    }

    public static void init() {
        menu = key("menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        zoom = key("zoom", GLFW.GLFW_KEY_C);
        fullbright = key("fullbright", GLFW.GLFW_KEY_B);
        perspective = key("perspective", GLFW.GLFW_KEY_Y);
        ptt = key("voice_ptt", GLFW.GLFW_KEY_V);
        mute = key("voice_mute", GLFW.GLFW_KEY_M);
        deafen = key("voice_deafen", GLFW.GLFW_KEY_U);
        for (int i = 0; i < 4; i++) AUTO[i] = key("autotext_" + (i + 1), GLFW.GLFW_KEY_KP_1 + i);
        loadAutotext();
        // Chat: la hora [HH:mm] de TODOS los mensajes la pone ChatHudMixin (Fabric API no trae MODIFY_CHAT en 1.21.11).
        // Este evento queda de respaldo para los mensajes del sistema si el mixin no se aplicara; stamp() no estampa dos veces.
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.MODIFY_GAME.register((msg, overlay) -> overlay ? msg : stamp(msg));
        ChatCopy.register();
    }

    /** Pone [HH:mm] delante del mensaje. Devuelve el MISMO objeto si no hay que tocarlo o si ya lo estampamos (para no duplicar la hora). */
    public static Text stamp(Text msg) {
        if (msg == null || msg == lastStamped) return msg;
        if (!Config.get("chat", true) || !Settings.b("chat", "time")) return msg;
        String f = Settings.b("chat", "seconds") ? "HH:mm:ss" : "HH:mm";
        Text out = Text.literal("[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern(f)) + "] ")
            .formatted(net.minecraft.util.Formatting.GRAY).append(msg);
        lastStamped = out;
        return out;
    }

    /** Lo usa ChatHudMixin en los dos topes de 100 de ChatHud. */
    public static int chatLimit(int vanilla) {
        return Config.get("chat", true) && Settings.b("chat", "long") ? CHAT_MAX : vanilla;
    }

    /** Multiplicador de zoom actual (lo lee ZoomFovMixin). */
    public static float zoomMul() { return zoomCur; }

    /** Rueda del mouse (ZoomScrollMixin): con la tecla de zoom pulsada cambia el nivel. true = el evento se consume (no cambia la ranura). */
    public static boolean zoomWheel(double v) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (v == 0 || zoom == null || mc.currentScreen != null || !zoom.isPressed()) return false;
        if (!Config.get("zoom", true) || !Settings.b("zoom", "wheel")) return false;
        float lvl = Settings.n("zoom", "level") + (v > 0 ? 0.5f : -0.5f);
        Settings.setNum("zoom", "level", Math.max(1.5f, Math.min(10f, lvl)));
        Config.save();
        return true;
    }

    /** <.minecraft>/atenas/autotext.txt: una linea por tecla (Numpad 1-4). Se crea con ejemplos. Lineas con "/" son comandos. */
    private static void loadAutotext() {
        File f = new File(SharedDir.dir(), "autotext.txt");
        try {
            if (!f.exists()) Files.writeString(f.toPath(), "gg\ngl hf\n\n\n", StandardCharsets.UTF_8);
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < 4; i++) AUTO_TEXT[i] = i < lines.size() ? lines.get(i).trim() : "";
        } catch (Exception ignored) { }
    }

    /** True si el codigo GLFW es la tecla del menu (RShift por defecto, o la que el jugador haya puesto). */
    public static boolean isMenuKey(int glfwKey) {
        try { return menu != null && KeyBindingHelper.getBoundKeyOf(menu).getCode() == glfwKey; } catch (Throwable t) { return glfwKey == GLFW.GLFW_KEY_RIGHT_SHIFT; }
    }

    private static boolean tabWas;

    /** Shift+Tab abre Amigos (como en 1.8.9): en el juego, en la pausa y en el menu principal. Se lee el teclado directo (Tab ya es la lista de jugadores). */
    private static void friendsHotkey(MinecraftClient mc) {
        if (mc.getOverlay() != null) return;
        boolean tab = com.atenas.fabric.gui.Keys.down(GLFW.GLFW_KEY_TAB);
        boolean shift = com.atenas.fabric.gui.Keys.shift();
        boolean edge = tab && shift && !tabWas;
        tabWas = tab;
        if (!edge) return;
        net.minecraft.client.gui.screen.Screen cs = mc.currentScreen;
        if (cs == null || cs instanceof net.minecraft.client.gui.screen.GameMenuScreen || cs instanceof net.minecraft.client.gui.screen.TitleScreen
                || cs instanceof com.atenas.fabric.AtenasTitleScreen) {
            mc.setScreen(new com.atenas.fabric.gui.FriendsScreen(cs));
        }
    }

    public static void tick(MinecraftClient mc) {
        friendsHotkey(mc);
        while (menu.wasPressed()) { if (mc.currentScreen == null) mc.setScreen(new ConfigScreen(null)); }
        if (mc.world == null || mc.player == null) {
            WaypointStore.setContext(null, 0);
            TeamSync.clear();
            VoiceClient.stop();
            zoomCur = 1f; legacyFov = false; fbOn = false;
            Freelook.reset();
            return;
        }
        // ---- contexto de mundo (claves iguales a 1.8.9) ----
        String server = null, key;
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            key = "sp:" + mc.getServer().getSaveProperties().getLevelName();
        } else {
            ServerInfo si = mc.getCurrentServerEntry();
            server = si != null && si.address != null ? si.address.trim().toLowerCase(Locale.ROOT) : null;
            key = "mp:" + (server != null ? server : "unknown");
        }
        String dimId = mc.world.getRegistryKey().getValue().toString();
        int dim = "minecraft:overworld".equals(dimId) ? 0 : "minecraft:the_nether".equals(dimId) ? -1 : "minecraft:the_end".equals(dimId) ? 1 : 2 + (dimId.hashCode() & 0xFFFF);
        WaypointStore.setContext(key, dim);

        // ---- zoom: mantener la tecla; la rueda cambia el nivel; suave; vuelve solo al soltar ----
        boolean z = Config.get("zoom", true) && zoom.isPressed() && mc.currentScreen == null;
        float zt = z ? Math.max(1.5f, Math.min(10f, Settings.n("zoom", "level"))) : 1f;
        zoomCur = Settings.b("zoom", "smooth") ? zoomCur + (zt - zoomCur) * 0.35f : zt;
        if (Math.abs(zoomCur - zt) < 0.02f) zoomCur = zt;
        if (!fovHooked) { // respaldo: si el mixin del FOV no esta, se baja la opcion de FOV (minimo 30)
            if (zoomCur > 1.01f) {
                if (!legacyFov) { savedFov = mc.options.getFov().getValue(); legacyFov = true; }
                mc.options.getFov().setValue(Math.max(30, Math.min(110, Math.round(savedFov / zoomCur))));
            } else if (legacyFov) { mc.options.getFov().setValue(savedFov); legacyFov = false; }
        }

        // ---- movimiento: sprint automatico y agacharse con toggle ----
        if (Config.get("movement", false)) {
            boolean auto = Settings.b("movement", "sprint") && mc.options.forwardKey.isPressed() && !mc.player.isSneaking() && mc.currentScreen == null;
            if (auto) { mc.options.sprintKey.setPressed(true); autoSprint = true; }
            else if (autoSprint) { mc.options.sprintKey.setPressed(false); autoSprint = false; }
            mc.options.getSneakToggled().setValue(Settings.b("movement", "sneak"));
        }

        // ---- perspectiva libre: mantener Y (o alternar) y la camara gira sin mover al personaje ----
        Freelook.tick(mc, perspective);

        // ---- fullbright: vision nocturna visual (se renueva sin particulas ni icono) ----
        while (fullbright.wasPressed()) {
            boolean on = Config.toggle("fullbright", false);
            if (!on) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        fbOn = Config.get("fullbright", false);
        if (fbOn && mc.player.age % 100 == 0) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
        }

        // ---- autotext ----
        for (int i = 0; i < 4; i++) {
            while (AUTO[i].wasPressed()) {
                String t = Config.get("autotext", true) ? Settings.str("autotext", "line" + (i + 1), AUTO_TEXT[i]) : null;
                if (t == null || t.isEmpty() || mc.currentScreen != null) continue;
                if (t.startsWith("/")) mc.player.networkHandler.sendChatCommand(t.substring(1));
                else mc.player.networkHandler.sendChatMessage(t);
            }
        }

        // ---- Team View ----
        String me = mc.getSession().getUsername();
        if (server != null && Social.validName(me) && Config.get("team", true)) {
            TeamSync.tick(me, server, Settings.b("team", "share"), Settings.b("team", "friends"), dim,
                mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getHealth(), mc.player.getMaxHealth());
        }

        // ---- voz ----
        while (mute.wasPressed()) VoiceClient.toggleMute();
        while (deafen.wasPressed()) VoiceClient.toggleDeafen();
        if (server != null && Social.validName(me) && Config.get("voice", true)) {
            VoiceClient.setTalking(ptt.isPressed() && mc.currentScreen == null);
            VoiceClient.update(me, server, dim, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        } else {
            VoiceClient.stop();
        }
    }

    public static List<String> toggleNames() {
        List<String> l = new ArrayList<>();
        for (String s : new String[] {"hud", "fps", "coords", "direction", "time", "waypoints", "team", "voice", "cps", "ping", "keystrokes", "armor", "potions", "speed", "memory", "clock", "fullbright", "combo", "reach", "crosshair", "nobg", "tablogo", "blockoverlay", "atenasmenu", "zoom", "perspective", "movement", "chat", "autotext"}) l.add(s);
        return l;
    }
}
