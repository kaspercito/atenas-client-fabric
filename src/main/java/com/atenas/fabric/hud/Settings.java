package com.atenas.fabric.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.atenas.fabric.shared.Config;

/** Ajustes por modulo (los de las tarjetas con engranaje del menu). Claves: "s.<modulo>.<nombre>" en fabric-config.properties. */
public final class Settings {
    private Settings() {}

    public enum T { BOOL, NUM, CHOICE, COLOR, TEXT }

    public static final class Def {
        public final String name, label;
        public final T type;
        public final double def, min, max, step;
        public final String[] opts;
        Def(String name, String label, T type, double def, double min, double max, double step, String[] opts) {
            this.name = name; this.label = label; this.type = type; this.def = def; this.min = min; this.max = max; this.step = step; this.opts = opts;
        }
    }

    public static final String[] COLOR_NAMES = {"Blanco", "Violeta", "Cian", "Verde", "Ambar", "Rojo", "Rosa", "Gris"};
    public static final int[] COLORS = {0xFFFFFFFF, 0xFFA78BFA, 0xFF22D3EE, 0xFF34D399, 0xFFFBBF24, 0xFFFB7185, 0xFFEC4899, 0xFF9B97B8};

    private static final Map<String, List<Def>> MAP = new LinkedHashMap<>();

    private static void add(String mod, Def d) { MAP.computeIfAbsent(mod, k -> new ArrayList<>()).add(d); }
    private static void bool(String mod, String n, String l, boolean def) { add(mod, new Def(n, l, T.BOOL, def ? 1 : 0, 0, 1, 1, null)); }
    private static void num(String mod, String n, String l, double def, double min, double max, double step) { add(mod, new Def(n, l, T.NUM, def, min, max, step, null)); }
    private static void choice(String mod, String n, String l, int def, String... o) { add(mod, new Def(n, l, T.CHOICE, def, 0, o.length - 1, 1, o)); }
    private static void color(String mod, String n, String l, int def) { add(mod, new Def(n, l, T.COLOR, def, 0, COLORS.length - 1, 1, COLOR_NAMES)); }
    private static void text(String mod, String n, String l) { add(mod, new Def(n, l, T.TEXT, 0, 0, 0, 0, null)); }

    static {
        bool("hud", "shadow", "Sombra del texto", true);
        color("hud", "color", "Color del texto", 0);
        bool("hud", "bg", "Fondo detras del texto", false);
        bool("team", "share", "Compartir mi posicion", true);
        bool("team", "friends", "Ver a mis amigos del cliente", true);
        bool("team", "hp", "Mostrar vida (corazones)", true);
        bool("team", "dist", "Mostrar distancia", true);
        bool("team", "status", "Mostrar estado de la conexion", true);
        color("team", "color", "Color de companeros", 3);
        bool("cps", "right", "Mostrar clic derecho", true);
        bool("armor", "percent", "Durabilidad en %", false);
        bool("potions", "time", "Mostrar tiempo", true);
        bool("keystrokes", "mouse", "Mostrar clics del mouse", true);
        bool("keystrokes", "space", "Mostrar barra espaciadora", true);
        choice("crosshair", "style", "Estilo", 0, "Cruz", "T", "Punto");
        num("crosshair", "size", "Tamano", 6, 1, 14, 1);
        num("crosshair", "gap", "Separacion", 3, 0, 10, 1);
        num("crosshair", "thick", "Grosor", 1, 1, 4, 1);
        color("crosshair", "color", "Color", 0);
        bool("crosshair", "outline", "Contorno negro", true);
        bool("crosshair", "aim", "Rojo al apuntar a una entidad", true);
        num("zoom", "level", "Nivel de zoom", 4, 1.5, 10, 0.5);
        bool("zoom", "wheel", "La rueda ajusta el zoom", true);
        bool("zoom", "smooth", "Zoom suave", true);
        choice("perspective", "mode", "Modo", 0, "Mantener", "Alternar");
        bool("perspective", "invert", "Invertir eje vertical", false);
        bool("movement", "sprint", "Sprint automatico", true);
        bool("movement", "sneak", "Agacharse con toggle", false);
        bool("chat", "time", "Hora [HH:mm] en cada mensaje", true);
        bool("chat", "seconds", "Incluir segundos", false);
        bool("chat", "long", "Chat largo (hasta 20 000 lineas)", true);
        bool("chat", "copy", "Copiar mensaje con clic derecho", true);
        bool("chat", "copynotime", "Copiar sin la hora", true);
        for (int i = 1; i <= 4; i++) text("autotext", "line" + i, "Numpad " + i + " (mensaje o /comando)");
    }

    public static boolean has(String mod) { return MAP.containsKey(mod); }
    public static List<Def> of(String mod) { return MAP.getOrDefault(mod, new ArrayList<>()); }

    private static Def def(String mod, String name) {
        for (Def d : of(mod)) if (d.name.equals(name)) return d;
        return null;
    }

    private static String key(String mod, String name) { return "s." + mod + "." + name; }

    public static boolean b(String mod, String name) { Def d = def(mod, name); return Config.get(key(mod, name), d != null && d.def != 0); }
    public static float n(String mod, String name) { Def d = def(mod, name); return Config.getFloat(key(mod, name), d == null ? 0f : (float) d.def); }
    public static int c(String mod, String name) { Def d = def(mod, name); return Config.getInt(key(mod, name), d == null ? 0 : (int) d.def); }
    public static int color(String mod, String name) { return COLORS[Math.max(0, Math.min(COLORS.length - 1, c(mod, name)))]; }
    public static String str(String mod, String name, String def) { return Config.getStr(key(mod, name), def); }

    public static void setBool(String mod, String name, boolean v) { Config.putBool(key(mod, name), v); Config.save(); }
    public static void setNum(String mod, String name, float v) { Config.putFloat(key(mod, name), v); }
    public static void setInt(String mod, String name, int v) { Config.putInt(key(mod, name), v); Config.save(); }
    public static void setStr(String mod, String name, String v) { Config.putStr(key(mod, name), v); }
}
