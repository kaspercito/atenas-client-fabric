package com.atenas.fabric.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.atenas.fabric.net.TeamSync;
import com.atenas.fabric.shared.Config;
import com.atenas.fabric.shared.Waypoint;
import com.atenas.fabric.shared.WaypointStore;
import com.atenas.fabric.voice.VoiceClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/**
 * HUD en texto (arriba a la izquierda) + lista de waypoints y companeros con flecha y distancia (arriba al centro).
 * Todavia no dibuja balizas dentro del mundo: eso depende del pipeline de render de 1.21.11 y va en el siguiente paso.
 */
public final class Hud {
    private Hud() {}

    private static final String[] ARROWS = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};
    private static final String[] DIRS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};

    private static String arrow(MinecraftClient mc, double dx, double dz) {
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = MathHelper.wrapDegrees(target - mc.player.getYaw());
        return ARROWS[((int) Math.round(rel / 45.0) + 8) % 8];
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden || !Config.get("hud", true)) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int white = Settings.color("hud", "color");
        boolean shadow = Settings.b("hud", "shadow"), bg = Settings.b("hud", "bg");
        int y = 0;
        List<String> left = new ArrayList<>();
        if (Config.get("fps", true)) left.add(mc.getCurrentFps() + " FPS");
        if (Config.get("coords", true)) left.add(String.format(Locale.ROOT, "XYZ %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        if (Config.get("direction", true)) left.add(DIRS[((int) Math.round(MathHelper.wrapDegrees(mc.player.getYaw()) / 45.0) + 8) % 8]);
        if (Config.get("time", true)) {
            long t = mc.world.getTimeOfDay() % 24000L;
            left.add(String.format(Locale.ROOT, "%02d:%02d", (t / 1000 + 6) % 24, (t % 1000) * 60 / 1000));
        }
        if (Config.get("voice", true)) {
            String v = VoiceClient.connected()
                ? (VoiceClient.muted() ? "Voz: silenciado" : VoiceClient.talking() ? "Voz: hablando" : "Voz: V para hablar")
                : "Voz: " + VoiceClient.status();
            if (VoiceClient.deafened()) v += " (sin audio)";
            left.add(v);
            String s = VoiceClient.lastSpeaker();
            if (!s.isEmpty()) left.add("\u266A " + s);
        }
        HudExtras.lines(mc, left);
        HudPos.begin(ctx, "info", sw, sh);
        for (String s : left) {
            if (bg) ctx.fill(-2, y - 1, mc.textRenderer.getWidth(s) + 2, y + 9, 0x80000000);
            ctx.drawText(mc.textRenderer, s, 0, y, white, shadow);
            y += 10;
        }
        HudPos.end(ctx);

        HudExtras.draw(mc, ctx);
        BlockOverlay.draw(mc, ctx, tick.getTickProgress(false));

        // ---- waypoints y companeros cercanos (arriba al centro) ----
        HudPos.begin(ctx, "wp", sw, sh);
        int cx = 80;
        int cy = 0;
        double px = mc.player.getX(), pz = mc.player.getZ();
        int dim = WaypointStore.currentDim();
        if (Config.get("waypoints", true)) {
            List<Waypoint> list = new ArrayList<>();
            for (Waypoint w : WaypointStore.current()) if (w.visible && w.dim == dim) list.add(w);
            list.sort(Comparator.comparingDouble(w -> (w.x - px) * (w.x - px) + (w.z - pz) * (w.z - pz)));
            for (int i = 0; i < Math.min(4, list.size()); i++) {
                Waypoint w = list.get(i);
                double dx = w.x - px, dz = w.z - pz;
                String s = String.format(Locale.ROOT, "%s %s %.0fm", arrow(mc, dx, dz), w.name, Math.sqrt(dx * dx + dz * dz));
                ctx.drawText(mc.textRenderer, s, cx - mc.textRenderer.getWidth(s) / 2, cy, 0xFF000000 | (w.color == 0 ? 0x55FFFF : w.color & 0xFFFFFF), true);
                cy += 10;
            }
        }
        if (Config.get("team", true)) {
            for (TeamSync.Mate m : TeamSync.mates().values()) {
                if (m.dim != dim) continue;
                double dx = m.x - px, dz = m.z - pz;
                String s = String.format(Locale.ROOT, "%s %s %.0fm  %.0f\u2764", arrow(mc, dx, dz), m.name, Math.sqrt(dx * dx + dz * dz), m.hp / 2f);
                ctx.drawText(mc.textRenderer, s, cx - mc.textRenderer.getWidth(s) / 2, cy, 0xFF55FF55, true);
                cy += 10;
            }
        }
        HudPos.end(ctx);
    }
}
