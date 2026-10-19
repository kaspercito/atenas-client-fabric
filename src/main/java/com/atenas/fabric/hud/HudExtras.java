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
        if (Config.get("cps", true)) out.add(Settings.b("cps", "right") ? "CPS " + cps(LEFT) + " | " + cps(RIGHT) : "CPS " + cps(LEFT));
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

    /** Armadura, pociones y teclas: cada una es un panel que se mueve y escala con el Editor HUD. */
    public static void draw(MinecraftClient mc, DrawContext ctx) {
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        int tc = Settings.color("hud", "color");
        boolean sh = Settings.b("hud", "shadow");
        if (Config.get("armor", true)) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            HudPos.begin(ctx, "armor", w, h);
            int y = 0;
            for (EquipmentSlot s : slots) {
                ItemStack st = mc.player.getEquippedStack(s);
                if (st.isEmpty()) continue;
                ctx.drawItem(st, 22, y);
                if (st.isDamageable()) {
                    int left = st.getMaxDamage() - st.getDamage();
                    String t = Settings.b("armor", "percent") ? (left * 100 / Math.max(1, st.getMaxDamage())) + "%" : String.valueOf(left);
                    int col = left * 4 < st.getMaxDamage() ? 0xFFFB7185 : tc;
                    ctx.drawText(mc.textRenderer, t, 20 - mc.textRenderer.getWidth(t), y + 5, col, sh);
                }
                y += 18;
            }
            HudPos.end(ctx);
        }
        if (Config.get("potions", true)) {
            HudPos.begin(ctx, "potions", w, h);
            int y = 0;
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                int sec = e.getDuration() / 20;
                String s = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1)
                    + (Settings.b("potions", "time") ? " " + (sec / 60) + ":" + String.format("%02d", sec % 60) : "");
                int col = sec <= 10 && (System.currentTimeMillis() / 400) % 2 == 0 ? 0xFFFB7185 : tc;
                ctx.drawText(mc.textRenderer, s, 110 - mc.textRenderer.getWidth(s), y, col, sh);
                y += 10;
            }
            HudPos.end(ctx);
        }
        if (Config.get("keystrokes", true)) {
            HudPos.begin(ctx, "keys", w, h);
            int k = 18;
            key(ctx, mc, "W", mc.options.forwardKey.isPressed(), k, 0, 16);
            key(ctx, mc, "A", mc.options.leftKey.isPressed(), 0, k, 16);
            key(ctx, mc, "S", mc.options.backKey.isPressed(), k, k, 16);
            key(ctx, mc, "D", mc.options.rightKey.isPressed(), 2 * k, k, 16);
            int y2 = 2 * k;
            if (Settings.b("keystrokes", "mouse")) {
                key(ctx, mc, "L " + cps(LEFT), mc.options.attackKey.isPressed(), 0, y2, 26);
                key(ctx, mc, "R " + cps(RIGHT), mc.options.useKey.isPressed(), 28, y2, 26);
                y2 += k;
            }
            if (Settings.b("keystrokes", "space")) {
                ctx.fill(0, y2, 54, y2 + 8, mc.options.jumpKey.isPressed() ? 0xAAFFFFFF : 0x66000000);
                ctx.fill(18, y2 + 3, 36, y2 + 4, mc.options.jumpKey.isPressed() ? 0xFF000000 : 0xFFFFFFFF);
            }
            HudPos.end(ctx);
        }
    }

    private static void key(DrawContext ctx, MinecraftClient mc, String t, boolean on, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 16, on ? 0xAAFFFFFF : 0x66000000);
        ctx.drawText(mc.textRenderer, t, x + w / 2 - mc.textRenderer.getWidth(t) / 2, y + 4, on ? 0xFF000000 : 0xFFFFFFFF, false);
    }
}
