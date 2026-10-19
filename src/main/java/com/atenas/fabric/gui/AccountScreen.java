package com.atenas.fabric.gui;

import com.atenas.fabric.net.Friends;
import com.atenas.fabric.net.Social;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Cuenta: nick actual, verificacion premium y estado del servicio de Atenas (amigos, cosmeticos, voz). */
public final class AccountScreen extends Screen {
    private final Screen parent;
    public AccountScreen(Screen parent) { super(Text.literal("Cuenta")); this.parent = parent; }

    @Override
    protected void init() {
        int cx = width / 2;
        addDrawableChild(new Ui.Btn(cx - 100, height / 2 + 30, 200, 22, Text.literal("Reintentar verificacion"), b -> { Social.retryVerify(); Friends.sync(false, ""); com.atenas.fabric.cosmetics.CosmeticSync.refreshNow(); }));
        addDrawableChild(new Ui.Btn(cx - 100, height / 2 + 56, 200, 22, Text.literal("Volver"), b -> close()));
        addDrawableChild(new Ui.Btn(width - 32, 10, 22, 18, Text.literal("X"), b -> close()));
    }

    @Override
    public void renderBackground(DrawContext g, int mx, int my, float d) { }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        Ui.backdrop(g, width, height);
        int cx = width / 2, y = height / 2 - 70;
        Ui.card(g, cx - 130, y - 12, 260, 130, Ui.PANEL, Ui.BORDER);
        Ui.centered(g, textRenderer, "A T E N A S   \u00b7   Cuenta", cx, y, Ui.ACCENT);
        String me = Friends.me();
        g.fill(cx - 90, y + 16, cx - 64, y + 42, Ui.VIOLET);
        Ui.centered(g, textRenderer, me.isEmpty() ? "?" : me.substring(0, 1).toUpperCase(), cx - 77, y + 25, 0xFF090812);
        Ui.text(g, textRenderer, me, cx - 58, y + 20, Ui.TEXT);
        boolean ver = Social.verified(me);
        Ui.text(g, textRenderer, ver ? "Cuenta verificada" : "Sin verificar (modo sin conexion)", cx - 58, y + 32, ver ? Ui.GREEN : Ui.AMBER);
        String st = Friends.usable() ? "Servicio de Atenas: conectado" : "error".equals(Friends.status()) ? "Servicio de Atenas: sin conexion" : "Servicio de Atenas: conectando...";
        Ui.text(g, textRenderer, st, cx - 110, y + 54, Friends.usable() ? Ui.GREEN : Ui.RED);
        String p = com.atenas.fabric.cosmetics.CosmeticSync.problem();
        Ui.text(g, textRenderer, Ui.fit(textRenderer, p == null ? "Cosmeticos: sincronizados" : p, 230), cx - 110, y + 66, p == null ? Ui.DIM : Ui.RED);
        super.render(g, mx, my, delta);
        Ui.paintButtons(g, textRenderer, children());
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
