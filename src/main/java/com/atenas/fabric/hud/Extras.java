package com.atenas.fabric.hud;

import java.util.Locale;

import com.atenas.fabric.shared.Config;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;

/** Combo, Reach, Crosshair propio, scoreboard sin fondo (el TAB lo resuelve PlayerListHudMixin). */
public final class Extras {
    private Extras() {}

    private static int combo, prevHurt;
    private static long lastHit, reachTime;
    private static double reach = -1;

    public static String comboText() { return "Combo: " + combo; }

    public static String reachText() {
        if (reach < 0 || System.currentTimeMillis() - reachTime > 5000L) return "Alcance: --";
        return String.format(Locale.ROOT, "Alcance: %.2f", reach);
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (world.isClient() && player == mc.player) {
                long now = System.currentTimeMillis();
                if (entity instanceof LivingEntity le && le.hurtTime <= 0) { combo++; lastHit = now; }
                if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == entity) { reach = ehr.getPos().distanceTo(player.getEyePos()); reachTime = now; }
            }
            return ActionResult.PASS;
        });

        // Mira propia (reemplaza la de vanilla)
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, orig -> (HudElement) (ctx, tick) -> {
            if (!Config.get("crosshair", true)) { orig.render(ctx, tick); return; }
            drawCrosshair(ctx);
        });
        // TAB (el de vanilla, como en 1.8.9; PlayerListHudMixin quita fondo/cuadros y pone el logo) y scoreboard sin fondo:
        // ademas se pone a 0 la opacidad del fondo de texto solo mientras se dibujan (respaldo si el mixin no encaja)
        HudElementRegistry.replaceElement(VanillaHudElements.PLAYER_LIST, orig -> (HudElement) (ctx, tick) -> noBg(orig, ctx, tick));
        HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, orig -> (HudElement) (ctx, tick) -> noBg(orig, ctx, tick));
    }

    private static void noBg(HudElement orig, DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.get("nobg", true)) { orig.render(ctx, tick); return; }
        double op = mc.options.getTextBackgroundOpacity().getValue();
        boolean only = mc.options.getBackgroundForChatOnly().getValue();
        mc.options.getTextBackgroundOpacity().setValue(0.0);
        mc.options.getBackgroundForChatOnly().setValue(false);
        try { orig.render(ctx, tick); }
        finally { mc.options.getTextBackgroundOpacity().setValue(op); mc.options.getBackgroundForChatOnly().setValue(only); }
    }

    private static void drawCrosshair(DrawContext g) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.getPerspective() != net.minecraft.client.option.Perspective.FIRST_PERSON || mc.options.hudHidden) return;
        int cx = g.getScaledWindowWidth() / 2, cy = g.getScaledWindowHeight() / 2;
        boolean aim = mc.crosshairTarget instanceof EntityHitResult;
        int c = aim && Settings.b("crosshair", "aim") ? 0xFFFF4D4D : Settings.color("crosshair", "color"), o = 0xFF000000;
        int s = (int) Settings.n("crosshair", "size"), gap = (int) Settings.n("crosshair", "gap"), t = (int) Settings.n("crosshair", "thick");
        int style = Settings.c("crosshair", "style"); // 0 cruz, 1 T, 2 punto
        boolean out = Settings.b("crosshair", "outline");
        int h = t / 2;
        if (style == 2) {
            if (out) g.fill(cx - h - 1, cy - h - 1, cx - h + t + 1, cy - h + t + 1, o);
            g.fill(cx - h, cy - h, cx - h + t, cy - h + t, c);
            return;
        }
        if (out) {
            g.fill(cx - gap - s - 1, cy - h - 1, cx - gap + 1, cy - h + t + 1, o);
            g.fill(cx + gap - 1, cy - h - 1, cx + gap + s + 1, cy - h + t + 1, o);
            g.fill(cx - h - 1, cy + gap - 1, cx - h + t + 1, cy + gap + s + 1, o);
            if (style == 0) g.fill(cx - h - 1, cy - gap - s - 1, cx - h + t + 1, cy - gap + 1, o);
        }
        g.fill(cx - gap - s, cy - h, cx - gap, cy - h + t, c);
        g.fill(cx + gap, cy - h, cx + gap + s, cy - h + t, c);
        g.fill(cx - h, cy + gap, cx - h + t, cy + gap + s, c);
        if (style == 0) g.fill(cx - h, cy - gap - s, cx - h + t, cy - gap, c);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.getOverlay() == null && mc.currentScreen != null && mc.currentScreen.getClass() == net.minecraft.client.gui.screen.TitleScreen.class && Config.get("atenasmenu", true))
            { System.out.println("[Atenas] menu principal de Atenas activado"); mc.setScreen(new com.atenas.fabric.AtenasTitleScreen()); }
        if (mc.player == null) { combo = 0; return; }
        int h = mc.player.hurtTime;
        if (h > 0 && prevHurt == 0) combo = 0;
        prevHurt = h;
        if (combo > 0 && System.currentTimeMillis() - lastHit > 3000L) combo = 0;
    }
}
