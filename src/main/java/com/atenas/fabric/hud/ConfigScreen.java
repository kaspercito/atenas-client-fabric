package com.atenas.fabric.hud;

import java.util.LinkedHashMap;
import java.util.Map;

import com.atenas.fabric.shared.Config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
        ITEMS.put("fullbright", new Object[] {"Fullbright", false});
    }

    public ConfigScreen() { super(Text.literal("Atenas Client")); }

    private static Text label(String key) {
        Object[] it = ITEMS.get(key);
        boolean on = Config.get(key, (Boolean) it[1]);
        return Text.literal(it[0] + ": " + (on ? "SI" : "NO"));
    }

    @Override
    protected void init() {
        int w = 150, h = 20, gap = 4;
        int x0 = width / 2 - w - gap / 2;
        int y0 = 40;
        int i = 0;
        for (String key : ITEMS.keySet()) {
            final String k = key;
            int x = x0 + (i % 2) * (w + gap);
            int y = y0 + (i / 2) * (h + gap);
            addDrawableChild(ButtonWidget.builder(label(k), b -> {
                boolean def = (Boolean) ITEMS.get(k)[1];
                boolean on = Config.toggle(k, def);
                if (k.equals("fullbright") && !on && client != null && client.player != null) {
                    client.player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
                }
                b.setMessage(label(k));
            }).dimensions(x, y, w, h).build());
            i++;
        }
        int rows = (ITEMS.size() + 1) / 2;
        int yb = y0 + rows * (h + gap) + 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("Cosmeticos"), b -> {
            com.atenas.fabric.cosmetics.CosmeticSync.refreshNow();
            if (client != null) client.setScreen(new com.atenas.fabric.cosmetics.CosmeticsScreen(this));
        }).dimensions(width / 2 - 75, yb, 150, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), b -> close())
            .dimensions(width / 2 - 75, yb + h + gap, 150, h).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Atenas Client 1.21.11"), width / 2, 16, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Esc para cerrar"), width / 2, 28, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldPause() { return false; }
}
