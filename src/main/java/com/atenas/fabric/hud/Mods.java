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
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;

/** Modulos con logica por tick: zoom, perspectiva, fullbright, autotext, voz, Team View y el contexto de mundo de los waypoints. */
public final class Mods {
    private Mods() {}

    private static final KeyBinding.Category CAT = KeyBinding.Category.create(Identifier.of("atenasclientfabric", "main"));
    private static KeyBinding menu, zoom, fullbright, perspective, ptt, mute, deafen;
    private static final KeyBinding[] AUTO = new KeyBinding[4];
    private static final String[] AUTO_TEXT = new String[4];

    private static boolean zooming; private static int savedFov;
    private static boolean fbOn;

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

    public static void tick(MinecraftClient mc) {
        while (menu.wasPressed()) { if (mc.currentScreen == null) mc.setScreen(new ConfigScreen()); }
        if (mc.world == null || mc.player == null) {
            WaypointStore.setContext(null, 0);
            TeamSync.clear();
            VoiceClient.stop();
            zooming = false; fbOn = false;
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

        // ---- zoom: mantener la tecla; baja el FOV y lo restaura al soltar ----
        boolean z = zoom.isPressed() && mc.currentScreen == null;
        if (z && !zooming) { savedFov = mc.options.getFov().getValue(); zooming = true; }
        if (z) mc.options.getFov().setValue(30);
        else if (zooming) { mc.options.getFov().setValue(savedFov); zooming = false; }

        // ---- perspectiva: alterna primera persona / tercera persona trasera ----
        while (perspective.wasPressed()) {
            mc.options.setPerspective(mc.options.getPerspective() == Perspective.FIRST_PERSON ? Perspective.THIRD_PERSON_BACK : Perspective.FIRST_PERSON);
        }

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
                String t = AUTO_TEXT[i];
                if (t == null || t.isEmpty() || mc.currentScreen != null) continue;
                if (t.startsWith("/")) mc.player.networkHandler.sendChatCommand(t.substring(1));
                else mc.player.networkHandler.sendChatMessage(t);
            }
        }

        // ---- Team View ----
        String me = mc.getSession().getUsername();
        if (server != null && Social.validName(me) && Config.get("team", true)) {
            TeamSync.tick(me, server, Config.get("team_share", true), Config.get("team_friends", true), dim,
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
        for (String s : new String[] {"hud", "fps", "coords", "direction", "time", "waypoints", "team", "team_share", "team_friends", "voice", "cps", "ping", "keystrokes", "armor", "potions", "speed", "memory", "clock", "fullbright", "combo", "reach", "crosshair", "nobg", "tablogo", "blockoverlay", "atenasmenu"}) l.add(s);
        return l;
    }
}
