package com.atenas.fabric.hud;

import com.atenas.fabric.gui.Fx;
import com.atenas.fabric.gui.Ui;
import com.atenas.fabric.shared.Config;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/** Editor HUD: arrastra cada panel con el mouse y cambia su tamano con la rueda. Se guarda al soltar. */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private int drag = -1;

    public HudEditorScreen(Screen parent) { super(Text.literal("Editor HUD")); this.parent = parent; }

    @Override
    protected void init() {
        addDrawableChild(new Ui.Btn(width / 2 - 104, height - 34, 100, 20, Text.literal("Restablecer"), b -> HudPos.reset()));
        addDrawableChild(new Ui.Btn(width / 2 + 4, height - 34, 100, 20, Text.literal("Listo"), b -> close()));
    }

    private int[] box(int i) {
        String id = HudPos.IDS[i];
        int[] a = HudPos.anchor(id, width, height), s = HudPos.size(id);
        float sc = HudPos.scale(id);
        return new int[] {(int) (a[0] + HudPos.dx(id)), (int) (a[1] + HudPos.dy(id)), (int) (s[0] * sc), (int) (s[1] * sc)};
    }

    private int hit(double mx, double my) {
        for (int i = HudPos.IDS.length - 1; i >= 0; i--) { int[] b = box(i); if (Ui.inside(mx, my, b[0], b[1], b[2], b[3])) return i; }
        return -1;
    }

    @Override
    public void renderBackground(DrawContext g, int mx, int my, float d) { }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        Ui.gradient(g, 0, 0, width, height, 0x80070914, 0xA00E0B22, true);
        Fx.centered(g, "Editor HUD  ·  arrastra para mover  ·  rueda para el tamano", width / 2, 8, Ui.DIM);
        int hov = hit(mx, my);
        for (int i = 0; i < HudPos.IDS.length; i++) {
            int[] b = box(i);
            boolean on = i == hov || i == drag;
            Ui.rect(g, b[0], b[1], b[2], b[3], on ? 0x408B5CF6 : 0x208B5CF6);
            Ui.border(g, b[0], b[1], b[2], b[3], on ? Ui.ACCENT : Ui.ACCENT_DIM);
            Ui.text(g, HudPos.NAMES[i] + "  " + Math.round(HudPos.scale(HudPos.IDS[i]) * 100) + "%", b[0] + 3, b[1] + 3, Ui.TEXT);
        }
        super.render(g, mx, my, delta);
        Ui.paintButtons(g, textRenderer, children());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() == 0) { drag = hit(click.x(), click.y()); return drag >= 0; }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (drag < 0) return super.mouseDragged(click, dx, dy);
        String id = HudPos.IDS[drag];
        HudPos.set(id, (float) (HudPos.dx(id) + dx), (float) (HudPos.dy(id) + dy), HudPos.scale(id));
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (drag >= 0) { drag = -1; Config.save(); return true; }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        int i = hit(mx, my);
        if (i < 0 || vy == 0) return false;
        String id = HudPos.IDS[i];
        HudPos.set(id, HudPos.dx(id), HudPos.dy(id), HudPos.scale(id) + (vy > 0 ? 0.1f : -0.1f));
        Config.save();
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public void close() { Config.save(); if (client != null) client.setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
