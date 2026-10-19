package com.atenas.fabric.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.atenas.fabric.net.Friends;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Amigos (igual que 1.8.9): lista agrupada (jugando / en el launcher / desconectados), solicitudes, anadir por nick y chat con el amigo elegido. */
public final class FriendsScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget addField, chatField;
    private String selected, note = "";
    private final List<Friends.Msg> msgs = new ArrayList<>();
    private long lastId;
    private long lastPoll;
    private int scroll;

    public FriendsScreen(Screen parent) { super(Text.literal("Amigos")); this.parent = parent; }

    @Override
    protected void init() {
        Friends.sync(false, "");
        int left = Math.max(10, width / 2 - 200), listW = 170;
        addField = new TextFieldWidget(textRenderer, left + 6, 46, listW - 62, 16, Text.literal("nick"));
        addField.setMaxLength(16); addField.setPlaceholder(Text.literal("A\u00f1adir amigo..."));
        addDrawableChild(addField);
        addDrawableChild(new Ui.Btn(left + listW - 52, 45, 52, 18, Text.literal("Anadir"), b -> {
            String n = addField.getText().trim();
            if (n.isEmpty()) return;
            addField.setText("");
            Friends.add(n, (r, e) -> note = e != null ? e : "Solicitud enviada a " + n);
        }));
        int cx = left + listW + 10, cw = 400 - listW - 10;
        chatField = new TextFieldWidget(textRenderer, cx + 4, height - 44, cw - 62, 16, Text.literal("mensaje"));
        chatField.setMaxLength(240); chatField.setPlaceholder(Text.literal("Escribe un mensaje..."));
        addDrawableChild(chatField);
        addDrawableChild(new Ui.Btn(cx + cw - 54, height - 45, 54, 18, Text.literal("Enviar"), b -> sendMsg()));
        addDrawableChild(new Ui.Btn(width - 32, 10, 22, 18, Text.literal("X"), b -> close()));
        addDrawableChild(new Ui.Btn(left, height - 28, 80, 18, Text.literal("Volver"), b -> close()));
    }

    private void sendMsg() {
        String t = chatField.getText().trim();
        if (t.isEmpty() || selected == null) return;
        chatField.setText("");
        final String to = selected;
        Friends.send(to, t, (m, e) -> MinecraftClient.getInstance().execute(() -> {
            if (e != null) note = e; else if (to.equals(selected)) { msgs.add(m); lastId = Math.max(lastId, m.id); }
        }));
    }

    private void select(Friends.Friend f) {
        selected = f.key; msgs.clear(); lastId = 0; lastPoll = 0;
        loadHistory();
    }

    private void loadHistory() {
        final String key = selected;
        if (key == null) return;
        Friends.history(key, lastId, (list, e) -> MinecraftClient.getInstance().execute(() -> {
            if (!key.equals(selected)) return;
            for (Friends.Msg m : list) if (m.id > lastId) { msgs.add(m); lastId = m.id; }
        }));
    }

    @Override
    public void tick() {
        if (Friends.sinceLastSync() > 6000) Friends.sync(false, "");
        long now = System.currentTimeMillis();
        if (selected != null && now - lastPoll > 3000) { lastPoll = now; loadHistory(); }
    }

    private static String stateText(Friends.Friend f) {
        if (f.playing()) return f.server.isEmpty() ? "Jugando" : "Jugando en " + f.server;
        return f.online() ? "En el launcher" : "Desconectado";
    }
    private static int stateColor(Friends.Friend f) { return f.playing() ? Ui.GREEN : f.online() ? Ui.CYAN : Ui.OFF; }

    private int rowAt(double mx, double my, List<Friends.Friend> sorted, int left) {
        if (mx < left + 6 || mx > left + 170) return -1;
        int idx = ((int) my - 70) / 26 + scroll;
        return my >= 70 && idx >= 0 && idx < sorted.size() ? idx : -1;
    }

    private List<Friends.Friend> sorted() {
        List<Friends.Friend> l = new ArrayList<>(Friends.snapshot().friends);
        l.sort((a, b) -> { int ra = a.playing() ? 0 : a.online() ? 1 : 2, rb = b.playing() ? 0 : b.online() ? 1 : 2; return ra != rb ? ra - rb : a.name.compareToIgnoreCase(b.name); });
        return l;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        int left = Math.max(10, width / 2 - 200);
        List<Friends.Friend> l = sorted();
        int i = rowAt(click.x(), click.y(), l, left);
        if (i >= 0) { select(l.get(i)); return true; }
        // solicitudes entrantes (aceptar al hacer clic)
        int y = 70 + Math.min(l.size() - scroll, 8) * 26 + 14;
        for (Friends.Req r : Friends.snapshot().incoming) {
            if (click.y() >= y && click.y() < y + 14 && click.x() >= left + 6 && click.x() < left + 170) {
                Friends.accept(r.key.isEmpty() ? r.name : r.key, (res, e) -> note = e != null ? e : "Ahora eres amigo de " + r.name);
                return true;
            }
            y += 16;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        int max = Math.max(0, sorted().size() - 8);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vy)));
        return true;
    }

    @Override
    public void renderBackground(DrawContext g, int mx, int my, float d) { }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        Ui.backdrop(g, width, height);
        int left = Math.max(10, width / 2 - 200), listW = 170, cx = left + listW + 10, cw = 400 - listW - 10;
        Ui.text(g, textRenderer, "A T E N A S   \u00b7   " + Friends.badge("Amigos"), left, 16, Ui.ACCENT);
        Ui.card(g, left, 38, listW, height - 70, Ui.PANEL, Ui.BORDER);
        Ui.card(g, cx, 38, cw, height - 70, Ui.PANEL, Ui.BORDER);

        List<Friends.Friend> l = sorted();
        int y = 70;
        for (int i = scroll; i < l.size() && i < scroll + 8; i++, y += 26) {
            Friends.Friend f = l.get(i);
            boolean sel = f.key.equals(selected), hov = mx >= left + 6 && mx < left + listW - 4 && my >= y && my < y + 24;
            Ui.card(g, left + 6, y, listW - 12, 24, sel ? Ui.PANEL2 : hov ? 0xFF191631 : Ui.PANEL, sel ? Ui.ACCENT : Ui.BORDER);
            int ring = stateColor(f);
            g.fill(left + 9, y + 3, left + 27, y + 21, ring);
            g.fill(left + 10, y + 4, left + 26, y + 20, 0xFF2A2550);
            Ui.centered(g, textRenderer, f.name.isEmpty() ? "?" : f.name.substring(0, 1).toUpperCase(Locale.ROOT), left + 18, y + 8, Ui.ACCENT);
            Ui.text(g, textRenderer, Ui.fit(textRenderer, f.name, listW - 70), left + 32, y + 4, f.online() ? Ui.TEXT : Ui.DIM);
            boolean prev = f.unread > 0 && !f.previewBody.isEmpty();
            Ui.text(g, textRenderer, Ui.fit(textRenderer, prev ? f.previewBody : stateText(f), listW - 50), left + 32, y + 14, prev ? Ui.AMBER : ring);
            if (f.unread > 0) Ui.text(g, textRenderer, String.valueOf(f.unread), left + listW - 18, y + 8, Ui.AMBER);
        }
        if (l.isEmpty()) Ui.text(g, textRenderer, "Aun no tienes amigos.", left + 10, 78, Ui.DIM);

        int ry = 70 + Math.min(Math.max(0, l.size() - scroll), 8) * 26 + 4;
        if (!Friends.snapshot().incoming.isEmpty()) {
            Ui.text(g, textRenderer, "SOLICITUDES (clic = aceptar)", left + 8, ry, Ui.PINK); ry += 10;
            for (Friends.Req r : Friends.snapshot().incoming) { Ui.text(g, textRenderer, "+ " + r.name, left + 10, ry + 2, Ui.TEXT); ry += 16; }
        }
        if (!Friends.snapshot().outgoing.isEmpty()) {
            Ui.text(g, textRenderer, "ENVIADAS", left + 8, ry + 2, Ui.DIM); ry += 12;
            for (Friends.Req r : Friends.snapshot().outgoing) { Ui.text(g, textRenderer, r.name, left + 10, ry, Ui.DIM); ry += 11; }
        }

        Friends.Friend sf = null;
        for (Friends.Friend f : l) if (f.key.equals(selected)) sf = f;
        if (sf == null) {
            Ui.centered(g, textRenderer, "Elige un amigo para chatear", cx + cw / 2, height / 2 - 4, Ui.DIM);
        } else {
            Ui.text(g, textRenderer, sf.name, cx + 8, 44, Ui.TEXT);
            Ui.text(g, textRenderer, stateText(sf), cx + 8, 55, stateColor(sf));
            g.fill(cx + 4, 66, cx + cw - 4, 67, Ui.BORDER);
            int my0 = height - 52, shown = (my0 - 72) / 11, from = Math.max(0, msgs.size() - shown);
            int yy = 72;
            for (int i = from; i < msgs.size(); i++, yy += 11) {
                Friends.Msg m = msgs.get(i);
                String s = Ui.fit(textRenderer, m.body, cw - 20);
                if (m.mine) Ui.text(g, textRenderer, s, cx + cw - 8 - textRenderer.getWidth(s), yy, Ui.ACCENT);
                else Ui.text(g, textRenderer, s, cx + 8, yy, Ui.TEXT);
            }
        }

        super.render(g, mx, my, delta);
        Ui.paintButtons(g, textRenderer, children());
        String st = Friends.usable() ? (note.isEmpty() ? "Conectado al servicio de amigos" : note) : "error".equals(Friends.status()) ? "Sin conexion con el servicio: " + Friends.error() : "Conectando...";
        Ui.text(g, textRenderer, Ui.fit(textRenderer, st, width - 120), left + 90, height - 23, Friends.usable() ? Ui.DIM : Ui.RED);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.key() == 257 || input.key() == 335) { // Enter
            if (chatField.isFocused()) { sendMsg(); return true; }
            if (addField.isFocused()) { String n = addField.getText().trim(); if (!n.isEmpty()) { addField.setText(""); Friends.add(n, (r, e) -> note = e != null ? e : "Solicitud enviada a " + n); } return true; }
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
