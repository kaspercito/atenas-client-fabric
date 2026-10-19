package com.atenas.fabric;

import java.io.InputStream;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Menu principal de Atenas (paleta violeta, logo, rejilla). Reemplaza al TitleScreen de vanilla. Boton ★ = servidor del pack (-Datenas.server). */
public final class AtenasTitleScreen extends Screen {
    private static final Identifier LOGO = Identifier.of("atenasclientfabric", "dyn/title_logo");
    private static boolean logoTried, logoOk;
    private final long t0 = System.currentTimeMillis();
    private final float[] sx = new float[60], sy = new float[60];

    public AtenasTitleScreen() {
        super(Text.literal("Atenas"));
        java.util.Random r = new java.util.Random(11);
        for (int i = 0; i < sx.length; i++) { sx[i] = r.nextFloat(); sy[i] = r.nextFloat() * 0.8f; }
    }

    private static String prop(String k) { String v = System.getProperty(k); return v == null ? "" : v.trim(); }

    @Override
    protected void init() {
        System.out.println("[Atenas] menu principal: init " + width + "x" + height);
        int w = 200, h = 20, x = width / 2 - w / 2, y = height / 2 - 10, gap = 24;
        String server = prop("atenas.server");
        if (!server.isEmpty()) {
            String name = prop("atenas.servername").isEmpty() ? "Atenas" : prop("atenas.servername");
            addDrawableChild(ButtonWidget.builder(Text.literal("\u2605 " + name), b -> join(server, name)).dimensions(x, y, w, h).build());
            y += gap;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Un jugador"), b -> client.setScreen(new SelectWorldScreen(this))).dimensions(x, y, w, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Multijugador"), b -> client.setScreen(new MultiplayerScreen(this))).dimensions(x, y + gap, w, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Opciones"), b -> client.setScreen(new OptionsScreen(this, client.options))).dimensions(x, y + gap * 2, 98, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Salir"), b -> client.scheduleStop()).dimensions(x + 102, y + gap * 2, 98, h).build());
    }

    private void join(String addr, String name) {
        try { ConnectScreen.connect(this, client, ServerAddress.parse(addr), new ServerInfo(name, addr, ServerInfo.ServerType.OTHER), false, null); }
        catch (Throwable t) { client.setScreen(new MultiplayerScreen(this)); }
    }

    private static int a(int rgb, float al) { return (Math.max(0, Math.min(255, (int) (al * 255f))) << 24) | (rgb & 0xFFFFFF); }

    @Override
    public void renderBackground(DrawContext g, int mx, int my, float delta) { /* se dibuja todo en render() */ }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        try { draw(g, mx, my, delta); }
        catch (Throwable t) {
            System.out.println("[Atenas] el menu de Atenas fallo al dibujar, se vuelve al de Minecraft: " + t);
            t.printStackTrace();
            com.atenas.fabric.shared.Config.set("atenasmenu", false);
            client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
        }
    }

    private boolean logged;

    private void draw(DrawContext g, int mx, int my, float delta) {
        if (!logged) { logged = true; System.out.println("[Atenas] menu principal: primer dibujo " + width + "x" + height); }
        float t = (System.currentTimeMillis() - t0) / 1000f;
        g.fillGradient(0, 0, width, height, 0xFF04030B, 0xFF120A2E);
        g.fillGradient(0, (int) (height * 0.55f), width, height, a(0x22D3EE, 0f), a(0x8B5CF6, 0.20f));
        float horizon = height * 0.58f, flow = (t * 0.2f) % 1f;
        for (int i = 0; i < 14; i++) {
            float p = (i + flow) / 14f;
            int gy = Math.round(horizon + (height - horizon) * p * p);
            g.fill(0, gy, width, gy + 1, a(0x8B5CF6, 0.05f + 0.20f * p));
        }
        for (int i = -10; i <= 10; i++) {
            float gx = width / 2f + i * (width / 16f);
            for (int yy = (int) horizon; yy < height; yy += 3) {
                float p = (yy - horizon) / (height - horizon);
                int xx = Math.round(width / 2f + (gx - width / 2f) * (0.15f + p * 1.1f));
                g.fill(xx, yy, xx + 1, yy + 1, a(0x22D3EE, 0.03f + 0.10f * p));
            }
        }
        for (int i = 0; i < sx.length; i++) {
            float tw = 0.25f + 0.55f * (0.5f + 0.5f * (float) Math.sin(t * 1.4f + i));
            g.fill((int) (sx[i] * width), (int) (sy[i] * height * 0.75f), (int) (sx[i] * width) + 1, (int) (sy[i] * height * 0.75f) + 1, a(0xFFFFFF, tw * 0.7f));
        }
        if (!logoTried) { logoTried = true; logoOk = loadLogo(); }
        int ls = Math.max(40, Math.min(80, height / 4)), ly = Math.max(8, height / 2 - 10 - ls - 36);
        if (logoOk) g.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, width / 2 - ls / 2, ly, 0f, 0f, ls, ls, ls, ls, ls, ls);
        String title = "A T E N A S", sub = "C L I E N T   \u00b7   1.21.11";
        g.drawText(textRenderer, title, width / 2 - textRenderer.getWidth(title) / 2, ly + ls + 6, 0xFFE9E3FF, false);
        g.drawText(textRenderer, sub, width / 2 - textRenderer.getWidth(sub) / 2, ly + ls + 18, 0xFF22D3EE, false);
        super.render(g, mx, my, delta);
        String who = client.getSession().getUsername();
        g.drawText(textRenderer, who, width - textRenderer.getWidth(who) - 8, height - 14, 0xFF9B97B8, false);
    }

    private boolean loadLogo() {
        try (InputStream in = AtenasTitleScreen.class.getResourceAsStream("/assets/atenasclientfabric/textures/gui/logo.png")) {
            if (in == null) return false;
            NativeImage img = NativeImage.read(in);
            MinecraftClient.getInstance().getTextureManager().registerTexture(LOGO, new NativeImageBackedTexture(() -> "atenas title logo", img));
            return true;
        } catch (Throwable t) { return false; }
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
