package com.atenas.fabric.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Paleta y piezas de dibujo de Atenas (las mismas que Theme/Draw/AtenasButton de 1.8.9): violeta sobre azul noche, cian de apoyo. */
public final class Ui {
    private Ui() {}

    public static final int BG = 0xF2090812, PANEL = 0xFF14122A, PANEL2 = 0xFF1E1B38, BORDER = 0xFF2B2848;
    public static final int ACCENT = 0xFFA78BFA, ACCENT_DIM = 0xFF6D4FC7, VIOLET = 0xFF8B5CF6, MAGENTA = 0xFFC026D3, PINK = 0xFFEC4899, CYAN = 0xFF22D3EE;
    public static final int GREEN = 0xFF34D399, AMBER = 0xFFFBBF24, RED = 0xFFFB7185;
    public static final int TEXT = 0xFFF1EEFC, DIM = 0xFF9B97B8, OFF = 0xFF3A3658;

    public static int a(int rgb, float al) { return (Math.max(0, Math.min(255, (int) (al * 255f))) << 24) | (rgb & 0xFFFFFF); }

    /** Rectangulo con las esquinas cortadas. */
    public static void chamfer(DrawContext g, int x, int y, int w, int h, int cut, int color) {
        g.fill(x + cut, y, x + w - cut, y + h, color);
        g.fill(x, y + cut, x + w, y + h - cut, color);
    }

    public static void chamferBorder(DrawContext g, int x, int y, int w, int h, int cut, int color) {
        g.fill(x + cut, y, x + w - cut, y + 1, color);
        g.fill(x + cut, y + h - 1, x + w - cut, y + h, color);
        g.fill(x, y + cut, x + 1, y + h - cut, color);
        g.fill(x + w - 1, y + cut, x + w, y + h - cut, color);
        for (int i = 0; i < cut; i++) {
            g.fill(x + cut - 1 - i, y + i, x + cut - i, y + i + 1, color);
            g.fill(x + w - cut + i, y + i, x + w - cut + i + 1, y + i + 1, color);
            g.fill(x + cut - 1 - i, y + h - 1 - i, x + cut - i, y + h - i, color);
            g.fill(x + w - cut + i, y + h - 1 - i, x + w - cut + i + 1, y + h - i, color);
        }
    }

    /** Degradado horizontal (a la izquierda c1, a la derecha c2). */
    public static void hgrad(DrawContext g, int x, int y, int w, int h, int c1, int c2) {
        int n = Math.max(1, Math.min(w, 48));
        for (int i = 0; i < n; i++) {
            float t = n == 1 ? 0 : i / (float) (n - 1);
            int x1 = x + i * w / n, x2 = x + (i + 1) * w / n;
            g.fill(x1, y, x2, y + h, mix(c1, c2, t));
        }
    }

    public static int mix(int c1, int c2, float t) {
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) { int p = (c1 >>> s) & 255, q = (c2 >>> s) & 255; r |= ((int) (p + (q - p) * t) & 255) << s; }
        return r;
    }

    public static void card(DrawContext g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, border); g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border); g.fill(x + w - 1, y, x + w, y + h, border);
    }

    public static void text(DrawContext g, TextRenderer tr, String s, int x, int y, int color) { g.drawText(tr, s, x, y, color, false); }
    public static void centered(DrawContext g, TextRenderer tr, String s, int cx, int y, int color) { g.drawText(tr, s, cx - tr.getWidth(s) / 2, y, color, false); }

    public static String fit(TextRenderer tr, String s, int w) {
        if (s == null) return "";
        if (tr.getWidth(s) <= w) return s;
        while (s.length() > 1 && tr.getWidth(s + "...") > w) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    /** Fondo de todas las pantallas propias: azul noche con degradado violeta y greca arriba. */
    public static void backdrop(DrawContext g, int w, int h) {
        g.fillGradient(0, 0, w, h, 0xFF04030B, 0xFF120A2E);
        g.fillGradient(0, (int) (h * 0.55f), w, h, a(0x22D3EE, 0f), a(0x8B5CF6, 0.20f));
        int ga = 0x558B5CF6;
        for (int gx = 0; gx + 12 <= w; gx += 12) {
            g.fill(gx, 3, gx + 8, 4, ga); g.fill(gx, 3, gx + 1, 9, ga); g.fill(gx, 8, gx + 5, 9, ga); g.fill(gx + 4, 5, gx + 5, 8, ga);
        }
        int c = 0xAA8B5CF6;
        g.fill(8, 14, 30, 15, c); g.fill(8, 14, 9, 36, c);
        g.fill(8, h - 9, 30, h - 8, c); g.fill(8, h - 30, 9, h - 8, c);
    }

    /** Switch del menu: encendido violeta/cian, apagado gris. */
    public static void toggle(DrawContext g, int x, int y, boolean on) {
        chamfer(g, x, y, 22, 10, 2, on ? a(0x8B5CF6, 0.9f) : 0xFF2A2748);
        g.fill(on ? x + 13 : x + 2, y + 2, on ? x + 20 : x + 9, y + 8, on ? CYAN : DIM);
    }

    /** Boton Atenas: cristal oscuro con esquinas cortadas, borde de neon y regla que crece desde el centro al pasar el mouse. */
    public static class Btn extends ButtonWidget {
        public float glow;
        public boolean small;
        public Btn(int x, int y, int w, int h, Text label, ButtonWidget.PressAction press) {
            super(x, y, w, h, label, press, DEFAULT_NARRATION_SUPPLIER);
            setAlpha(0f); // el widget de vanilla queda invisible: lo dibujamos nosotros encima (paint)
        }
        public void paint(DrawContext g, TextRenderer tr) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean on = isHovered() && active;
            glow += ((on ? 1f : 0f) - glow) * 0.22f;
            int cut = h >= 18 ? 4 : 3;
            chamfer(g, x, y, w, h, cut, active ? 0xE60D0B1E : 0xA0090812);
            g.fillGradient(x + cut, y + h / 2, x + w - cut, y + h - 1, 0x00A78BFA, ((int) (0x46 * glow) << 24) | 0x8B5CF6);
            chamferBorder(g, x, y, w, h, cut, on ? ACCENT : (active ? 0xFF3A3466 : 0xFF241F3C));
            int bar = (int) (glow * (w - 2 * cut));
            if (bar > 0) {
                hgrad(g, x + w / 2 - bar / 2, y + h - 2, bar / 2 + 1, 2, 0x008B5CF6, VIOLET);
                hgrad(g, x + w / 2, y + h - 2, bar / 2, 2, CYAN, 0x0022D3EE);
            }
            g.fill(x + w - cut - 5, y, x + w - cut, y + 1, on ? CYAN : ACCENT_DIM);
            g.fill(x + cut, y + h - 1, x + cut + 5, y + h, on ? PINK : ACCENT_DIM);
            int tc = !active ? OFF : on ? 0xFFFFFFFF : TEXT;
            String s = fit(tr, getMessage().getString(), w - 2 * cut - 4);
            g.drawText(tr, s, x + w / 2 - tr.getWidth(s) / 2, y + (h - 8) / 2, tc, false);
        }
    }

    public static void paintButtons(DrawContext g, TextRenderer tr, Iterable<? extends net.minecraft.client.gui.Element> children) {
        for (net.minecraft.client.gui.Element e : children) if (e instanceof Btn b) b.paint(g, tr);
    }
}
