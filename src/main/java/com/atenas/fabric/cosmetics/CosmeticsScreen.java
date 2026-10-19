package com.atenas.fabric.cosmetics;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Menu de capas: cuadricula paginada. Lo que eliges se guarda y se sube; lo elegido en 1.8.9 / 1.20.1 aparece al abrirlo. */
public final class CosmeticsScreen extends Screen {
    private static final int COLS = 3, ROWS = 6, PER_PAGE = COLS * ROWS;
    private final Screen parent;
    private int page;

    public CosmeticsScreen(Screen parent) { super(Text.literal("Cosmeticos")); this.parent = parent; }

    @Override
    protected void init() {
        List<Cosmetics.Item> list = new ArrayList<>();
        for (Cosmetics.Item i : Cosmetics.capes()) if (!i.locked) list.add(i);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        int w = 120, h = 20, gap = 4, x0 = width / 2 - (COLS * w + (COLS - 1) * gap) / 2, y0 = 44;
        String sel = Cosmetics.selectedId();
        for (int n = 0; n < PER_PAGE; n++) {
            int idx = page * PER_PAGE + n;
            if (idx >= list.size()) break;
            final Cosmetics.Item it = list.get(idx);
            String label = (it.id.equals(sel) ? "\u2713 " : "") + it.name;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> { Cosmetics.select(it.id); clearAndInit(); })
                .dimensions(x0 + (n % COLS) * (w + gap), y0 + (n / COLS) * (h + gap), w, h).build());
        }
        int yb = y0 + ROWS * (h + gap) + 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); clearAndInit(); }).dimensions(x0, yb, 40, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(pages - 1, page + 1); clearAndInit(); }).dimensions(x0 + COLS * (w + gap) - gap - 40, yb, 40, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sin capa"), b -> { Cosmetics.select(null); clearAndInit(); }).dimensions(width / 2 - 75, yb, 70, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Volver"), b -> close()).dimensions(width / 2 + 5, yb, 70, h).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Capas de Atenas"), width / 2, 16, 0xFFFFFFFF);
        String p = CosmeticSync.problem();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(p != null ? p : "Sincronizado con el cliente 1.8.9 y el mod 1.20.1"), width / 2, 30, p != null ? 0xFFFF7777 : 0xFFAAAAAA);
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
