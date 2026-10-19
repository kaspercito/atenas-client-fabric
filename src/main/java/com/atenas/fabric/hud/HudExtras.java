package com.atenas.fabric.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.atenas.fabric.shared.Config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

/** Modulos del cliente 1.8.9 que SI sirven en 1.21.11: CPS, ping, velocidad, memoria, reloj, armadura, pociones y teclas. */
public final class HudExtras {
    private HudExtras() {}

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final long[] LEFT = new long[64], RIGHT = new long[64];
    private static int li, ri;
    private static boolean wasL, wasR;
    private static double lastX, lastZ, speed;

    /** Cada tick: cuenta clics (por transicion de la tecla) y mide la velocidad. */
    public static void tick(MinecraftClient mc) {
        if (mc.player == null) return;
        boolean l = mc.options.attackKey.isPressed(), r = mc.options.useKey.isPressed();
        long now = System.currentTimeMillis();
        if (l && !wasL) LEFT[li++ & 63] = now;
        if (r && !wasR) RIGHT[ri++ & 63] = now;
        wasL = l; wasR = r;
        double dx = mc.player.getX() - lastX, dz = mc.player.getZ() - lastZ;
        speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
        lastX = mc.player.getX(); lastZ = mc.player.getZ();
    }

    private static int cps(long[] a) {
        long now = System.currentTimeMillis();
        int n = 0;
        for (long t : a) if (t != 0 && now - t < 1000) n++;
        return n;
    }

    /** Lineas de texto para la columna izquierda del HUD. */
    public static void lines(MinecraftClient mc, List<String> out) {
        if (Config.get("cps", true)) out.add("CPS " + cps(LEFT) + " | " + cps(RIGHT));
        if (Config.get("ping", true) && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) out.add("Ping " + e.getLatency() + " ms");
        }
        if (Config.get("combo", true)) out.add(Extras.comboText());
        if (Config.get("reach", true)) out.add(Extras.reachText());
        if (Config.get("speed", false)) out.add(String.format(Locale.ROOT, "%.1f b/s", speed));
        if (Config.get("memory", false)) {
            Runtime rt = Runtime.getRuntime();
            out.add("RAM " + (rt.totalMemory() - rt.freeMemory()) / 1048576L + "/" + rt.maxMemory() / 1048576L + " MB");
        }
        if (Config.get("clock", false)) out.add(LocalTime.now().format(CLOCK));
    }

    /** Armadura (abajo a la derecha), pociones (derecha) y teclas (abajo a la izquierda). */
    public static void draw(MinecraftClient mc, DrawContext ctx) {
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        if (Config.get("armor", true)) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int y = h / 2 - 32;
            for (EquipmentSlot s : slots) {
                ItemStack st = mc.player.getEquippedStack(s);
                if (st.isEmpty()) continue;
                ctx.drawItem(st, w - 22, y);
                if (st.isDamageable()) ctx.drawText(mc.textRenderer, String.valueOf(st.getMaxDamage() - st.getDamage()), w - 24 - mc.textRenderer.getWidth(String.valueOf(st.getMaxDamage() - st.getDamage())), y + 5, 0xFFFFFFFF, true);
                y += 18;
            }
        }
        if (Config.get("potions", true)) {
            List<String> ps = new ArrayList<>();
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                int sec = e.getDuration() / 20;
                ps.add(e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1) + " " + (sec / 60) + ":" + String.format("%02d", sec % 60));
            }
            int y = 4;
            for (String s : ps) { ctx.drawText(mc.textRenderer, s, w - 4 - mc.textRenderer.getWidth(s), y, 0xFFFFFFFF, true); y += 10; }
        }
        if (Config.get("keystrokes", true)) {
            int x = 8, y = h - 62, k = 18;
            key(ctx, mc, "W", mc.options.forwardKey.isPressed(), x + k, y);
            key(ctx, mc, "A", mc.options.leftKey.isPressed(), x, y + k);
            key(ctx, mc, "S", mc.options.backKey.isPressed(), x + k, y + k);
            key(ctx, mc, "D", mc.options.rightKey.isPressed(), x + 2 * k, y + k);
        }
    }

    private static void key(DrawContext ctx, MinecraftClient mc, String t, boolean on, int x, int y) {
        ctx.fill(x, y, x + 16, y + 16, on ? 0xAAFFFFFF : 0x66000000);
        ctx.drawText(mc.textRenderer, t, x + 8 - mc.textRenderer.getWidth(t) / 2, y + 4, on ? 0xFF000000 : 0xFFFFFFFF, false);
    }
}
