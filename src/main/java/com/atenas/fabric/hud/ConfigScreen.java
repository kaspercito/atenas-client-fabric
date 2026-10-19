package com.atenas.fabric.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.atenas.fabric.gui.FriendsScreen;
import com.atenas.fabric.gui.Ui;
import com.atenas.fabric.net.Friends;
import com.atenas.fabric.shared.Config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Menu de Atenas Client (Right Shift): un boton por modulo que alterna activado/desactivado y guarda al instante. */
public final class ConfigScreen extends Screen {
    // clave de Config -> [nombre visible, activado por defecto]
    private static final Map<String, Object[]> ITEMS = new LinkedHashMap<>();
    static {
        ITEMS.put("hud", new Object[] {"HUD", true});
        ITEMS.put("fps", new Object[] {"FPS", true});
        ITEMS.put("coords", new Object[] {"Coordenadas", true});
        ITEMS.put("direction", new Object[] {"Direccion", true});
        ITEMS.put("time", new Object[] {"Hora", true});
        ITEMS.put("waypoints", new Object[] {"Waypoints", true});
        ITEMS.put("team", new Object[] {"Team View", true});
        ITEMS.put("team_share", new Object[] {"Compartir mi posicion", true});
        ITEMS.put("team_friends", new Object[] {"Ver a mis amigos", true});
        ITEMS.put("voice", new Object[] {"Voice chat", true});
        ITEMS.put("cps", new Object[] {"CPS", true});
        ITEMS.put("ping", new Object[] {"Ping", true});
        ITEMS.put("keystrokes", new Object[] {"Teclas", true});
        ITEMS.put("armor", new Object[] {"Armadura", true});
        ITEMS.put("potions", new Object[] {"Pociones", true});
        ITEMS.put("speed", new Object[] {"Velocidad", false});
        ITEMS.put("memory", new Object[] {"Memoria", false});
        ITEMS.put("clock", new Object[] {"Reloj real", false});
        ITEMS.put("combo", new Object[] {"Combo", true});
        ITEMS.put("reach", new Object[] {"Alcance (Reach)", true});
        ITEMS.put("crosshair", new Object[] {"Mira propia", true});
        ITEMS.put("nobg", new Object[] {"TAB/Score sin fondo", true});
        ITEMS.put("tablogo", new Object[] {"Logo en TAB/nametag", true});
        ITEMS.put("blockoverlay", new Object[] {"Resalte de bloque", true});
        ITEMS.put("atenasmenu", new Object[] {"Men\u00fa Atenas", true});
        ITEMS.put("fullbright", new Object[] {"Fullbright", false});
    }

    // categoria y descripcion por modulo (como las tarjetas de 1.8.9)
    private static final String[] CATS = {"Todos", "HUD", "PvP", "Rendimiento", "Social", "Visual"};
    private static final int[] CAT_COL = {Ui.ACCENT, Ui.CYAN, Ui.RED, Ui.GREEN, Ui.VIOLET, Ui.AMBER};
    private static final Map<String, Object[]> META = new java.util.HashMap<>();
    private static void m(String k, int cat, String d) { META.put(k, new Object[] {cat, d}); }
    static {
        m("hud", 1, "Muestra u oculta todo el HUD de Atenas."); m("fps", 1, "Fotogramas por segundo en pantalla.");
        m("coords", 1, "Tus coordenadas X, Y y Z."); m("direction", 1, "Hacia donde miras (N, S, E, O).");
        m("time", 1, "Hora del mundo de Minecraft."); m("waypoints", 1, "Marcas compartidas con 1.8.9 y 1.20.1.");
        m("team", 4, "Ve la posicion de tu equipo en pantalla."); m("team_share", 4, "Comparte tu posicion con tus amigos.");
        m("team_friends", 4, "Muestra a tus amigos del cliente."); m("voice", 4, "Chat de voz por proximidad.");
        m("cps", 2, "Clics por segundo."); m("ping", 1, "Latencia con el servidor.");
        m("keystrokes", 2, "Teclas W A S D y clics en pantalla."); m("armor", 2, "Durabilidad de tu armadura.");
        m("potions", 2, "Efectos de pocion activos."); m("speed", 1, "Tu velocidad en bloques por segundo.");
        m("memory", 3, "Uso de memoria de Java."); m("clock", 1, "Hora real de tu equipo.");
        m("combo", 2, "Golpes seguidos sin recibir dano."); m("reach", 2, "Distancia de tu ultimo golpe.");
        m("crosshair", 5, "Mira propia del cliente."); m("nobg", 5, "TAB y marcador sin fondo.");
        m("tablogo", 4, "Logo de Atenas junto a los usuarios del cliente."); m("blockoverlay", 5, "Resalte del bloque que apuntas.");
        m("atenasmenu", 5, "Usa el menu principal de Atenas."); m("fullbright", 5, "Brillo maximo para ver de noche.");
    }

    private static final int SIDE = 104;
    private int tab, filter, scroll, x0, y0, pw, ph;
    private net.minecraft.client.gui.widget.TextFieldWidget search;

    private static int cat(String k) { return (Integer) META.getOrDefault(k, new Object[] {1, ""})[0]; }
    private static String desc(String k) { return (String) META.getOrDefault(k, new Object[] {1, ""})[1]; }
    private boolean on(String key) { Object[] it = ITEMS.get(key); return Config.get(key, (Boolean) it[1]); }

    private void flip(String k) {
        boolean def = (Boolean) ITEMS.get(k)[1];
        boolean on = Config.toggle(k, def);
        if (k.equals("fullbright") && !on && client != null && client.player != null) {
            client.player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
        }
    }

    private void geometry() {
        pw = Math.min(600, width - 24); ph = Math.min(330, height - 24);
        x0 = (width - pw) / 2; y0 = (height - ph) / 2;
    }

    @Override
    protected void init() {
        geometry();
        String keep = search == null ? "" : search.getText();
        int sw = 120, bx = x0 + SIDE + 10, bw = pw - SIDE - 20;
        search = new net.minecraft.client.gui.widget.TextFieldWidget(textRenderer, bx + bw - sw + 2, y0 + 12, sw - 4, 12, Text.literal("buscar"));
        search.setDrawsBackground(false); search.setMaxLength(24); search.setText(keep);
        search.setPlaceholder(Text.literal("Buscar..."));
        addDrawableChild(search);
        Friends.sync(false, "");
    }

    private List<String> visibleKeys() {
        List<String> out = new ArrayList<>();
        String q = search == null ? "" : search.getText().trim().toLowerCase(java.util.Locale.ROOT);
        for (String k : ITEMS.keySet()) {
            if (filter > 0 && cat(k) != filter) continue;
            String n = ((String) ITEMS.get(k)[0]).toLowerCase(java.util.Locale.ROOT);
            if (!q.isEmpty() && !n.contains(q) && !desc(k).toLowerCase(java.util.Locale.ROOT).contains(q)) continue;
            out.add(k);
        }
        return out;
    }

    private int cols(int bw) { return bw >= 540 ? 3 : bw >= 300 ? 2 : 1; }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int bx = x0 + SIDE + 10, by = y0 + 10, bw = pw - SIDE - 20, bh = ph - 20;
        // barra lateral
        int step = Math.max(17, Math.min(25, (ph - 78) / 5)), rowH = step - 3, y = y0 + 50;
        for (int i = 0; i < 5; i++, y += step) {
            if (mx >= x0 + 6 && mx < x0 + SIDE - 6 && my >= y && my < y + rowH) {
                switch (i) {
                    case 0: tab = 0; break;
                    case 1: com.atenas.fabric.cosmetics.CosmeticSync.refreshNow(); client.setScreen(new com.atenas.fabric.cosmetics.CosmeticsScreen(this)); break;
                    case 2: tab = 0; filter = 1; scroll = 0; break; // Editor HUD: pendiente, abre la categoria HUD
                    case 3: client.setScreen(new FriendsScreen(this)); break;
                    default: tab = 1; break;
                }
                return true;
            }
        }
        if (tab == 0) {
            int cx = bx, chipY = by + 20;
            for (int c = 0; c < CATS.length; c++) {
                int w = textRenderer.getWidth(CATS[c]) + 16;
                if (cx + w > bx + bw) break;
                if (mx >= cx && mx < cx + w && my >= chipY && my < chipY + 15) { filter = c; scroll = 0; return true; }
                cx += w + 4;
            }
            int top = by + 42, listH = bh - 42, cols = cols(bw), gap = 8, cw = (bw - 6 - gap * (cols - 1)) / cols, chh = 56;
            List<String> keys = visibleKeys();
            if (my >= top && my < top + listH) for (int i = 0; i < keys.size(); i++) {
                int x = bx + (i % cols) * (cw + gap), yy = top + (i / cols) * (chh + gap) - scroll;
                if (mx >= x && mx < x + cw && my >= yy && my < yy + chh) { flip(keys.get(i)); return true; }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        int bh = ph - 20, bw = pw - SIDE - 20, cols = cols(bw), rows = (visibleKeys().size() + cols - 1) / cols;
        int total = Math.max(0, rows * 64 - 8), listH = bh - 42;
        scroll = Math.max(0, Math.min(Math.max(0, total - listH), scroll - (int) (vy * 24)));
        return true;
    }

    @Override
    public void renderBackground(DrawContext g, int mx, int my, float d) { }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        geometry();
        g.fillGradient(0, 0, width, height, 0xB0070610, 0xC0140A2A);
        Ui.card(g, x0 - 3, y0 - 3, pw + 6, ph + 6, 0x30000000, 0x18A78BFA);
        Ui.card(g, x0, y0, pw, ph, Ui.BG, Ui.BORDER);
        // barra lateral
        g.fill(x0 + 1, y0 + 2, x0 + SIDE, y0 + ph - 1, 0xFF0C0A1A);
        g.fill(x0 + SIDE, y0 + 2, x0 + SIDE + 1, y0 + ph - 1, Ui.BORDER);
        g.fill(x0 + 10, y0 + 12, x0 + 38, y0 + 40, Ui.a(0x8B5CF6, 0.35f));
        Ui.card(g, x0 + 10, y0 + 12, 28, 28, 0x00000000, Ui.VIOLET);
        Ui.text(g, textRenderer, "A", x0 + 22, y0 + 22, Ui.ACCENT);
        Ui.text(g, textRenderer, "ATENAS", x0 + 44, y0 + 14, Ui.ACCENT);
        Ui.text(g, textRenderer, "CLIENT", x0 + 44, y0 + 25, Ui.DIM);
        String[] labels = {"Mods", "Cosm\u00e9ticos", "Editor HUD", Friends.badge("Amigos"), "Acerca de"};
        int step = Math.max(17, Math.min(25, (ph - 78) / labels.length)), rowH = step - 3, y = y0 + 50;
        for (int i = 0; i < labels.length; i++, y += step) {
            boolean active = (i == 0 && tab == 0) || (i == 4 && tab == 1);
            boolean hover = mx >= x0 + 6 && mx < x0 + SIDE - 6 && my >= y && my < y + rowH;
            if (active) { Ui.card(g, x0 + 6, y, SIDE - 12, rowH, Ui.PANEL2, Ui.BORDER); g.fillGradient(x0 + 6, y + 3, x0 + 8, y + rowH - 3, Ui.VIOLET, Ui.PINK); }
            else if (hover) Ui.card(g, x0 + 6, y, SIDE - 12, rowH, 0xFF191631, 0xFF191631);
            Ui.text(g, textRenderer, Ui.fit(textRenderer, labels[i], SIDE - 28), x0 + 16, y + (rowH - 8) / 2, active || hover ? Ui.TEXT : Ui.DIM);
        }
        if (y + 6 <= y0 + ph - 24) {
            Ui.hgrad(g, x0 + 10, y0 + ph - 24, SIDE - 20, 1, Ui.ACCENT_DIM, 0x006D4FC7);
            Ui.text(g, textRenderer, "v" + com.atenas.fabric.AtenasFabricClient.VERSION, x0 + 12, y0 + ph - 16, Ui.DIM);
        }
        Ui.hgrad(g, x0 + 1, y0, pw - 2, 2, Ui.VIOLET, Ui.PINK);

        int bx = x0 + SIDE + 10, by = y0 + 10, bw = pw - SIDE - 20, bh = ph - 20;
        if (tab == 1) { drawAbout(g, bx, by, bw); search.visible = false; super.render(g, mx, my, delta); return; }
        search.visible = true;
        Ui.text(g, textRenderer, "Mods", bx, by + 2, Ui.TEXT);
        int sw = 120, sx = bx + bw - sw;
        Ui.card(g, sx, by - 2, sw, 16, Ui.PANEL, search.isFocused() ? Ui.ACCENT : Ui.BORDER);
        int cx = bx, chipY = by + 20;
        for (int c = 0; c < CATS.length; c++) {
            int w = textRenderer.getWidth(CATS[c]) + 16;
            if (cx + w > bx + bw) break;
            boolean onC = c == filter, hov = mx >= cx && mx < cx + w && my >= chipY && my < chipY + 15;
            Ui.card(g, cx, chipY, w, 15, onC ? Ui.PANEL2 : hov ? 0xFF191631 : Ui.PANEL, onC ? CAT_COL[c] : Ui.BORDER);
            if (c > 0) g.fill(cx + 5, chipY + 6, cx + 8, chipY + 9, CAT_COL[c]);
            Ui.text(g, textRenderer, CATS[c], cx + (c == 0 ? 8 : 11), chipY + 4, onC ? Ui.TEXT : Ui.DIM);
            cx += w + 4;
        }
        int top = by + 42, listH = bh - 42, cols = cols(bw), gap = 8, cw = (bw - 6 - gap * (cols - 1)) / cols, chh = 56;
        List<String> keys = visibleKeys();
        int rows = (keys.size() + cols - 1) / cols, total = Math.max(0, rows * (chh + gap) - gap);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - listH)));
        if (keys.isEmpty()) Ui.centered(g, textRenderer, "No hay mods con ese filtro.", bx + bw / 2, top + 30, Ui.DIM);
        g.enableScissor(bx, top, bx + bw, top + listH);
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            int x = bx + (i % cols) * (cw + gap), yy = top + (i / cols) * (chh + gap) - scroll;
            if (yy + chh < top || yy > top + listH) continue;
            int col = CAT_COL[cat(k)];
            boolean onK = on(k), hov = mx >= x && mx < x + cw && my >= Math.max(yy, top) && my < Math.min(yy + chh, top + listH);
            Ui.card(g, x, yy, cw, chh, hov ? Ui.PANEL2 : Ui.PANEL, onK ? col : hov ? Ui.ACCENT_DIM : Ui.BORDER);
            if (onK) g.fillGradient(x + 1, yy + 1, x + cw - 1, yy + chh - 1, 0x00000000, (0x26 << 24) | (col & 0xFFFFFF));
            String name = (String) ITEMS.get(k)[0];
            g.fill(x + 8, yy + 8, x + 30, yy + 30, (0x40 << 24) | (col & 0xFFFFFF));
            Ui.card(g, x + 8, yy + 8, 22, 22, 0x00000000, col);
            Ui.centered(g, textRenderer, name.substring(0, 1).toUpperCase(), x + 19, yy + 15, Ui.TEXT);
            Ui.text(g, textRenderer, Ui.fit(textRenderer, name, cw - 36 - 34), x + 37, yy + 9, Ui.TEXT);
            Ui.text(g, textRenderer, CATS[cat(k)], x + 37, yy + 20, col);
            Ui.toggle(g, x + cw - 30, yy + 8, onK);
            Ui.text(g, textRenderer, Ui.fit(textRenderer, desc(k), cw - 16), x + 8, yy + 38, Ui.DIM);
        }
        g.disableScissor();
        if (total > listH) {
            int barH = Math.max(18, listH * listH / total), barY = top + (int) ((listH - barH) * (scroll / (double) (total - listH)));
            g.fill(bx + bw - 3, top, bx + bw - 1, top + listH, 0x20FFFFFF);
            g.fillGradient(bx + bw - 3, barY, bx + bw - 1, barY + barH, Ui.VIOLET, Ui.PINK);
        }
        super.render(g, mx, my, delta);
    }

    private void drawAbout(DrawContext g, int bx, int by, int bw) {
        Ui.text(g, textRenderer, "Acerca de", bx, by + 2, Ui.TEXT);
        Ui.card(g, bx, by + 20, bw, 110, Ui.PANEL, Ui.BORDER);
        Ui.text(g, textRenderer, "Atenas Client", bx + 12, by + 32, Ui.ACCENT);
        Ui.text(g, textRenderer, "Version " + com.atenas.fabric.AtenasFabricClient.VERSION + "  \u00b7  Minecraft 1.21.11 (Fabric)", bx + 12, by + 46, Ui.DIM);
        Ui.text(g, textRenderer, "Cosmeticos, amigos y waypoints compartidos con 1.8.9 y 1.20.1.", bx + 12, by + 62, Ui.DIM);
        Ui.text(g, textRenderer, "Menu: Right Shift", bx + 12, by + 78, Ui.DIM);
    }

    @Override
    public boolean shouldPause() { return false; }
}
