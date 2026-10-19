package com.atenas.fabric.hud;

import com.atenas.fabric.shared.Config;

import net.minecraft.client.gui.DrawContext;

/** Paneles del HUD que se pueden mover y escalar con el Editor HUD. Cada uno se dibuja en coordenadas locales (0,0) dentro de begin/end. */
public final class HudPos {
    private HudPos() {}

    public static final String[] IDS = {"info", "armor", "potions", "keys", "wp"};
    public static final String[] NAMES = {"Informacion", "Armadura", "Pociones", "Teclas", "Waypoints"};

    /** Tamano aproximado del panel a escala 1 (para la caja del editor). */
    public static int[] size(String id) {
        switch (id) {
            case "armor": return new int[] {40, 72};
            case "potions": return new int[] {110, 44};
            case "keys": return new int[] {56, 56};
            case "wp": return new int[] {160, 40};
            default: return new int[] {118, 70};
        }
    }

    /** Posicion por defecto (esquina superior izquierda del panel). */
    public static int[] anchor(String id, int sw, int sh) {
        switch (id) {
            case "armor": return new int[] {sw - 44, sh / 2 - 32};
            case "potions": return new int[] {sw - 114, 4};
            case "keys": return new int[] {8, sh - 66};
            case "wp": return new int[] {sw / 2 - 80, 4};
            default: return new int[] {4, 4};
        }
    }

    public static float dx(String id) { return Config.getFloat("hp." + id + ".x", 0f); }
    public static float dy(String id) { return Config.getFloat("hp." + id + ".y", 0f); }
    public static float scale(String id) { return Math.max(0.5f, Math.min(2.5f, Config.getFloat("hp." + id + ".s", 1f))); }

    public static void set(String id, float dx, float dy, float s) {
        Config.putFloat("hp." + id + ".x", dx); Config.putFloat("hp." + id + ".y", dy); Config.putFloat("hp." + id + ".s", s);
    }

    public static void reset() { for (String id : IDS) set(id, 0f, 0f, 1f); Config.save(); }

    public static void begin(DrawContext g, String id, int sw, int sh) {
        int[] a = anchor(id, sw, sh);
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(a[0] + dx(id), a[1] + dy(id));
        float s = scale(id);
        g.getMatrices().scale(s, s);
    }

    public static void end(DrawContext g) { g.getMatrices().popMatrix(); }
}
