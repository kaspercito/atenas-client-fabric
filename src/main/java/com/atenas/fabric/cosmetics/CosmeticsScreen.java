package com.atenas.fabric.cosmetics;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Menu de cosmeticos con el estilo de Atenas: pestanas Capas / Alas / Sombreros, cuadricula paginada y VISTA PREVIA con tu personaje
 * (se ve la capa, las alas y el sombrero que tienes puestos, y cambia al instante al elegir).
 */
public final class CosmeticsScreen extends Screen {
    private static final String[] TYPES = {"cape", "wings", "hat", "pet"};
    private static final String[] LABELS = {"Capas", "Alas", "Sombreros", "Mascotas"};
    private static final int COLS = 2, ROWS = 8, PER_PAGE = COLS * ROWS;
    private static int tab;
    private final Screen parent;
    private int page;

    public CosmeticsScreen(Screen parent) { super(Text.literal("Cosmeticos")); this.parent = parent; }

    @Override
    protected void init() {
        String type = TYPES[tab];
        List<Cosmetics.Item> list = new ArrayList<>();
        for (Cosmetics.Item i : Cosmetics.listOf(type)) if (!i.locked) list.add(i);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        int panelW = 340, left = width / 2 - panelW / 2;
        for (int t = 0; t < 4; t++) {
            final int tt = t;
            addDrawableChild(ButtonWidget.builder(Text.literal((t == tab ? "\u25B6 " : "") + LABELS[t]), b -> { tab = tt; page = 0; clearAndInit(); })
                .dimensions(left + t * 62, 34, 60, 18).build());
        }
        int w = 100, h = 18, gap = 3, x0 = left, y0 = 58;
        String sel = Cosmetics.selectedId(type);
        for (int n = 0; n < PER_PAGE; n++) {
            int idx = page * PER_PAGE + n;
            if (idx >= list.size()) break;
            final Cosmetics.Item it = list.get(idx);
            String label = (it.id.equals(sel) ? "\u2713 " : "") + it.name;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> { Cosmetics.select(type, it.id); clearAndInit(); })
                .dimensions(x0 + (n % COLS) * (w + gap), y0 + (n / COLS) * (h + gap), w, h).build());
        }
        int yb = y0 + ROWS * (h + gap) + 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); clearAndInit(); }).dimensions(x0, yb, 30, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(pages - 1, page + 1); clearAndInit(); }).dimensions(x0 + 2 * (w + gap) - gap - 30, yb, 30, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Quitar"), b -> { Cosmetics.select(type, null); clearAndInit(); }).dimensions(x0 + 34, yb, 66, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Volver"), b -> close()).dimensions(x0 + 103, yb, 66, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xFF04030B, 0xFF1A0F3D);
        int panelW = 340, left = width / 2 - panelW / 2;
        ctx.fill(left - 8, 26, left + panelW + 8, height - 12, 0xB00B0E1C);
        ctx.fill(left - 8, 26, left + panelW + 8, 27, 0xFF8B5CF6);
        // vista previa con el personaje
        int px1 = left + 215, px2 = left + panelW, py1 = 58, py2 = height - 40;
        ctx.fill(px1, py1, px2, py2, 0x80120A2E);
        if (client != null && client.player != null) {
            try { InventoryScreen.drawEntity(ctx, px1, py1, px2, py2, Math.max(30, (py2 - py1) / 3), 0.0625f, mouseX, mouseY, client.player); } catch (Throwable ignored) { }
        }
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawText(textRenderer, "A T E N A S   \u00b7   Cosm\u00e9ticos", left, 12, 0xFFE9E3FF, false);
        String p = CosmeticSync.problem();
        ctx.drawText(textRenderer, p != null ? p : "Sincronizado con 1.8.9 y 1.20.1", left, height - 24, p != null ? 0xFFFF7777 : 0xFF9B97B8, false);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Vista previa"), (px1 + px2) / 2, py1 - 10, 0xFF22D3EE);
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
