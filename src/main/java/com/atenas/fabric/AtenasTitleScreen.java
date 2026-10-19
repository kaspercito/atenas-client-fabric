package com.atenas.fabric;

import java.io.InputStream;

import com.atenas.fabric.gui.Ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
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

    private int top, logoS, bw, bh, gap, btnY, sh, brandH;
    private float nameScale;
    private Ui.Btn friendsBtn;

    private void layout() {
        boolean hasServer = !prop("atenas.server").isEmpty();
        int avail = height - 14 - (height < 300 ? 26 : 46);
        float[][] tiers = {{64, 3f, 112, 24, 26, 5, 4}, {40, 2f, 82, 12, 22, 4, 4}, {28, 1.5f, 64, 8, 20, 3, 3}, {0, 1.5f, 32, 6, 18, 2, 3}};
        int rows = hasServer ? 4 : 3, total = 0;
        for (int t = 0; t < tiers.length; t++) {
            float[] v = tiers[t];
            logoS = (int) v[0]; nameScale = v[1]; brandH = (int) v[2]; int sep = (int) v[3]; bh = (int) v[4]; gap = (int) v[5]; sh = bh - (int) v[6]; bw = 250;
            total = brandH + sep + rows * bh + (rows - 1) * gap + 8 + sh;
            if (total <= avail || t == tiers.length - 1) { btnY = brandH + sep; break; }
        }
        top = Math.max(8, 14 + (avail - total) / 2);
        btnY += top;
    }

    @Override
    protected void init() {
        layout();
        int x = width / 2 - bw / 2, y = btnY;
        String server = prop("atenas.server");
        if (!server.isEmpty()) {
            String name = prop("atenas.servername").isEmpty() ? "Atenas" : prop("atenas.servername");
            addDrawableChild(new Ui.Btn(x, y, bw, bh, Text.literal("\u2605 " + name), b -> join(server, name)));
            y += bh + gap;
        }
        addDrawableChild(new Ui.Btn(x, y, bw, bh, Text.literal("Un jugador"), b -> client.setScreen(new SelectWorldScreen(this))));
        addDrawableChild(new Ui.Btn(x, y + bh + gap, bw, bh, Text.literal("Multijugador"), b -> client.setScreen(new MultiplayerScreen(this))));
        addDrawableChild(new Ui.Btn(x, y + 2 * (bh + gap), bw, bh, Text.literal("Opciones"), b -> client.setScreen(new OptionsScreen(this, client.options))));
        int y2 = y + 3 * (bh + gap) - gap + 8, third = (bw - 12) / 3;
        friendsBtn = new Ui.Btn(x, y2, third, sh, Text.literal("Amigos"), b -> client.setScreen(new com.atenas.fabric.gui.FriendsScreen(this)));
        addDrawableChild(friendsBtn);
        addDrawableChild(new Ui.Btn(x + third + 6, y2, third, sh, Text.literal("Cosm\u00e9ticos"), b -> {
            com.atenas.fabric.cosmetics.CosmeticSync.refreshNow();
            client.setScreen(new com.atenas.fabric.cosmetics.CosmeticsScreen(this));
        }));
        addDrawableChild(new Ui.Btn(x + 2 * (third + 6), y2, bw - 2 * (third + 6), sh, Text.literal("Cuenta"), b -> client.setScreen(new com.atenas.fabric.gui.AccountScreen(this))));
        addDrawableChild(new Ui.Btn(width - 32, 10, 22, 18, Text.literal("X"), b -> client.scheduleStop()));
        com.atenas.fabric.net.Friends.sync(false, "");
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
        int nameY = top;
        if (logoS > 0) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.003));
            for (int gg = 3; gg >= 1; gg--) {
                int gs = logoS + gg * 10;
                g.fill(width / 2 - gs / 2, top + logoS / 2 - gs / 2, width / 2 + gs / 2, top + logoS / 2 + gs / 2, a(0x8B5CF6, (8 + 10 * pulse) / gg / 255f));
            }
            if (logoOk) g.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, width / 2 - logoS / 2, top, 0f, 0f, logoS, logoS, logoS, logoS, logoS, logoS);
            nameY = top + logoS + 6;
        }
        String title = "ATENAS";
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(width / 2f, (float) nameY);
        g.getMatrices().scale(nameScale, nameScale);
        g.drawText(textRenderer, title, -textRenderer.getWidth(title) / 2, 0, Ui.ACCENT, true);
        g.getMatrices().popMatrix();
        int subY = nameY + (int) (8 * nameScale) + 4;
        String sub = "C   L   I   E   N   T";
        g.drawText(textRenderer, sub, width / 2 - textRenderer.getWidth(sub) / 2, subY, Ui.CYAN, false);
        g.fill(width / 2 - 60, subY + 12, width / 2 + 60, subY + 13, Ui.ACCENT_DIM);
        g.fill(width / 2 - 8, subY + 11, width / 2 + 8, subY + 14, Ui.VIOLET);

        if (friendsBtn != null) friendsBtn.setMessage(Text.literal(com.atenas.fabric.net.Friends.badge("Amigos")));
        super.render(g, mx, my, delta);
        Ui.paintButtons(g, textRenderer, children());

        // esquinas HUD y greca (igual que 1.8.9)
        int ga = 0x558B5CF6;
        for (int gx = 0; gx + 12 <= width; gx += 12) {
            g.fill(gx, 3, gx + 8, 4, ga); g.fill(gx, 3, gx + 1, 9, ga); g.fill(gx, 8, gx + 5, 9, ga); g.fill(gx + 4, 5, gx + 5, 8, ga);
        }
        int c = 0xAA8B5CF6;
        g.fill(8, 14, 30, 15, c); g.fill(8, 14, 9, 36, c);
        g.fill(8, height - 9, 30, height - 8, c); g.fill(8, height - 30, 9, height - 8, c);

        // pie: pack/version (izquierda), atajo (centro), cuenta (derecha)
        int fy = height < 300 ? height - 20 : height - 22;
        String who = client.getSession().getUsername();
        int chipW = textRenderer.getWidth(who) + 30, chipX = width - chipW - 14;
        Ui.chamfer(g, chipX, fy - 6, chipW, 18, 3, 0xE60D0B1E);
        Ui.chamferBorder(g, chipX, fy - 6, chipW, 18, 3, 0xFF3A3466);
        g.fill(chipX + 5, fy - 2, chipX + 15, fy + 8, com.atenas.fabric.net.Friends.usable() ? Ui.GREEN : Ui.OFF);
        g.drawText(textRenderer, who.isEmpty() ? "?" : who.substring(0, 1).toUpperCase(), chipX + 8, fy, 0xFF090812, false);
        g.drawText(textRenderer, who, chipX + 21, fy, Ui.TEXT, false);
        g.drawText(textRenderer, "Atenas Client " + AtenasFabricClient.VERSION + "  \u00b7  1.21.11", 18, fy, Ui.DIM, false);
        String fr = com.atenas.fabric.net.Friends.badge("Amigos y cosm\u00e9ticos arriba");
        int frW = textRenderer.getWidth(fr), frX = width / 2 - frW / 2;
        if (frX > 18 + 190 && frX + frW + 14 < chipX) g.drawText(textRenderer, fr, frX, fy, Ui.DIM, false);
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
