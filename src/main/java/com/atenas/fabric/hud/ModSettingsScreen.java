package com.atenas.fabric.hud;

import java.util.ArrayList;
import java.util.List;

import com.atenas.fabric.gui.Fx;
import com.atenas.fabric.gui.Ui;
import com.atenas.fabric.shared.Config;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/** Panel de ajustes de un modulo (interruptores, deslizadores, listas, colores y textos). Todo se guarda al instante. */
public final class ModSettingsScreen extends Screen {
    private static final int ROW = 24;
    private final Screen parent;
    private final String mod, title;
    private final List<Settings.Def> defs;
    private final List<TextFieldWidget> fields = new ArrayList<>();
    private int px, py, pw, ph, dragging = -1;

    public ModSettingsScreen(Screen parent, String mod, String title) {
        super(Text.literal(title));
        this.parent = parent; this.mod = mod; this.title = title;
        this.defs = Settings.of(mod);
    }

    @Override
    protected void init() {
        pw = Math.min(380, width - 16);
        ph = Math.min(52 + defs.size() * ROW + 34, height - 8);
        px = (width - pw) / 2; py = (height - ph) / 2;
        fields.clear();
        for (int i = 0; i < defs.size(); i++) {
            Settings.Def d = defs.get(i);
            if (d.type != Settings.T.TEXT) continue;
            final String name = d.name;
            TextFieldWidget f = new TextFieldWidget(textRenderer, px + pw - 176 + 5, rowY(i) + 8, 156, 8, Text.empty());
            f.setDrawsBackground(false);
            f.setMaxLength(100);
            f.setText(Settings.str(mod, name, ""));
            f.setChangedListener(t -> Settings.setStr(mod, name, t));
            addDrawableChild(f);
            fields.add(f);
        }
        addDrawableChild(new Ui.Btn(px + pw - 14 - 90, py + ph - 28, 90, 20, Text.literal("Listo"), b -> close()));
    }

    private int rowY(int i) { return py + 44 + i * ROW; }
    private int ctlX() { return px + pw - 176; }

    @Override
    public void renderBackground(DrawContext g, int mx, int my, float d) { }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        Ui.gradient(g, 0, 0, width, height, 0xC0070914, 0xE00E0B22, true);
        Ui.card(g, px - 3, py - 3, pw + 6, ph + 6, 0x30000000, 0x188B5CF6);
        Ui.card(g, px, py, pw, ph, 0xF00A0D1C, Ui.BORDER);
        Ui.gradient(g, px + 1, py, pw - 2, 2, Ui.VIOLET, Ui.PINK, false);
        Fx.heading(g, title, px + 14, py + 12, Ui.TEXT);
        Ui.text(g, "Ajustes del modulo", px + 14, py + 26, Ui.DIM);
        Ui.rect(g, px + 14, py + 40, pw - 28, 1, Ui.BORDER);
        int fi = 0;
        for (int i = 0; i < defs.size(); i++) {
            Settings.Def d = defs.get(i);
            int y = rowY(i), cx = ctlX();
            Ui.text(g, Fx.fit(d.label, cx - px - 24), px + 14, y + 8, Ui.TEXT);
            switch (d.type) {
                case BOOL -> Ui.toggle(g, px + pw - 14 - 22, y + 6, Settings.b(mod, d.name));
                case NUM -> {
                    float v = Settings.n(mod, d.name);
                    double t = (v - d.min) / (d.max - d.min);
                    Ui.rect(g, cx, y + 11, 120, 2, Ui.BORDER);
                    Ui.rect(g, cx, y + 11, (int) (120 * t), 2, Ui.VIOLET);
                    Ui.rect(g, cx + (int) (120 * t) - 2, y + 7, 5, 10, Ui.ACCENT);
                    Ui.text(g, d.step < 1 ? String.format(java.util.Locale.ROOT, "%.1f", v) : String.valueOf(Math.round(v)), cx + 128, y + 8, Ui.DIM);
                }
                case CHOICE, COLOR -> {
                    int idx = Settings.c(mod, d.name);
                    boolean hov = Ui.inside(mx, my, cx, y + 2, 156, 18);
                    Ui.card(g, cx, y + 2, 156, 18, hov ? Ui.PANEL2 : Ui.PANEL, hov ? Ui.ACCENT : Ui.BORDER);
                    if (d.type == Settings.T.COLOR) Ui.rect(g, cx + 6, y + 7, 8, 8, Settings.COLORS[Math.max(0, Math.min(idx, Settings.COLORS.length - 1))]);
                    Ui.text(g, d.opts[Math.max(0, Math.min(idx, d.opts.length - 1))], cx + (d.type == Settings.T.COLOR ? 20 : 8), y + 8, Ui.TEXT);
                    Ui.text(g, ">", cx + 146, y + 8, Ui.DIM);
                }
                case TEXT -> {
                    TextFieldWidget f = fields.get(fi++);
                    Ui.card(g, cx, y + 2, 166, 18, 0xFF0A0918, f.isFocused() ? Ui.ACCENT : Ui.BORDER);
                }
            }
        }
        super.render(g, mx, my, delta);
        Ui.paintButtons(g, textRenderer, children());
    }

    private void slide(int i, double mx) {
        Settings.Def d = defs.get(i);
        double t = Math.max(0, Math.min(1, (mx - ctlX()) / 120.0));
        double v = d.min + t * (d.max - d.min);
        v = Math.round(v / d.step) * d.step;
        Settings.setNum(mod, d.name, (float) v);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;
        double mx = click.x(), my = click.y();
        int fi = 0;
        for (int i = 0; i < defs.size(); i++) {
            Settings.Def d = defs.get(i);
            int y = rowY(i), cx = ctlX();
            if (my < y || my >= y + ROW) { if (d.type == Settings.T.TEXT) fi++; continue; }
            switch (d.type) {
                case BOOL -> { if (mx >= px + pw - 40) Settings.setBool(mod, d.name, !Settings.b(mod, d.name)); }
                case NUM -> { if (mx >= cx - 4 && mx <= cx + 124) { dragging = i; slide(i, mx); } }
                case CHOICE, COLOR -> { if (mx >= cx && mx < cx + 156) Settings.setInt(mod, d.name, (Settings.c(mod, d.name) + 1) % d.opts.length); }
                case TEXT -> { if (mx >= cx && mx < cx + 166) { setFocused(fields.get(fi)); return true; } }
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging >= 0) { slide(dragging, click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging >= 0) { dragging = -1; Config.save(); return true; }
        return super.mouseReleased(click);
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
