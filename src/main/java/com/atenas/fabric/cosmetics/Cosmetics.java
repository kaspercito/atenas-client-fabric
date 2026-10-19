package com.atenas.fabric.cosmetics;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.atenas.fabric.shared.SharedDir;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Capas de Atenas (mismos ids que el cliente 1.8.9 y el mod 1.20.1, asi lo que equipas en uno se ve en los otros).
 * Eleccion guardada en <.minecraft>/atenas/cosmetics.json. Alas, sombreros y mascotas: pendientes (ver LEEME).
 */
public final class Cosmetics {
    private Cosmetics() {}

    public static final String NS = "atenasclientfabric";
    public static final int FRAME_MS = 90;
    /** Los cosmeticos de otros jugadores solo se dibujan a menos de 40 bloques (al cuadrado). */
    public static final double MAX_DIST_SQ = 40.0 * 40.0;
    public static volatile boolean animate = true;

    public static final class Item {
        public final String id, name;
        public final int color, frames, ver;
        public final boolean glow, custom;
        /** Privado y tu nick no esta en su lista: no sale en tu menu. */
        public boolean locked;
        Item(String id, String name, int color, boolean glow, int frames, boolean custom, int ver) {
            this.id = id; this.name = name; this.color = color; this.glow = glow; this.frames = Math.max(1, frames); this.custom = custom; this.ver = ver;
        }
    }

    /** Entrada de capa del catalogo del servicio. */
    public static final class Def {
        public final String id, name; public final int color, frames, ver; public final boolean glow; public final Set<String> owners;
        public Def(String id, String name, int color, boolean glow, int frames, int ver, Set<String> owners) {
            this.id = id; this.name = name; this.color = color; this.glow = glow; this.frames = frames; this.ver = ver; this.owners = owners;
        }
    }

    private static final List<Item> BUILTIN = new ArrayList<>();
    private static final List<Item> CAPES = new CopyOnWriteArrayList<>();
    /** Id elegido (null = ninguna). Puede ser un id del catalogo que aun no se bajo: se conserva hasta que llegue. */
    private static volatile String selected;
    private static volatile boolean loaded;

    private static void b(String id, String name, int color, boolean glow, int frames) { BUILTIN.add(new Item(id, name, color, glow, frames, false, 0)); }

    static {
        b("atenas", "Atenas (Exclusiva)", 0xFFF5C542, true, 12);
        b("atardecer", "Atardecer Lunar", 0xFFFF7A59, false, 1);
        b("aurora", "Aurora", 0xFF35E0A8, true, 12);
        b("sakura", "Sakura", 0xFFF8A9C8, false, 1);
        b("oceano", "Oc\u00e9ano al Atardecer", 0xFF1E7FD8, false, 1);
        b("bosque", "Bosque Brumoso", 0xFF4F8F72, false, 1);
        b("galaxia", "Galaxia", 0xFF7B4DFF, true, 12);
        b("lava", "Monta\u00f1a de Lava", 0xFFFF6A1A, true, 12);
        b("hielo", "Cristal de Hielo", 0xFF7FD6FF, false, 1);
        b("llamas", "Llamas", 0xFFFF7A1A, true, 12);
        b("matrix", "Matrix", 0xFF1BFF5A, true, 12);
        b("pulso", "Pulso Synthwave", 0xFFFF2DA6, true, 12);
        b("rey", "Corona Real", 0xFFF5C542, true, 12);
        b("rayo", "Tormenta El\u00e9ctrica", 0xFFFFE23D, true, 12);
        b("calavera", "Calavera Infernal", 0xFFE8503A, true, 12);
        b("espadas", "Escudo de Guerra", 0xFF2457C5, true, 12);
        b("luna", "Luna M\u00e1gica", 0xFFF4F1DE, true, 12);
        b("corazon", "Coraz\u00f3n Latiendo", 0xFFFF4F8B, true, 12);
        b("diamante", "Diamante Radiante", 0xFF5FD8FF, true, 12);
        b("brujula", "Br\u00fajula M\u00edstica", 0xFFD4AF37, true, 12);
        b("sol", "Sol Naciente", 0xFFFFB400, true, 12);
        b("trebol", "Tr\u00e9bol Encantado", 0xFF2ECC71, true, 12);
        b("arcoiris", "Arco\u00edris", 0xFF9B7BFF, true, 12);
        b("mareas", "Mareas", 0xFF2E6FC4, true, 12);
        b("eclipse", "Eclipse Solar", 0xFFFFB84D, true, 12);
        b("tormenta", "Tormenta", 0xFF5B7CFA, true, 12);
        b("nieve", "Ventisca", 0xFFBFE3FF, true, 12);
        b("luciernagas", "Luci\u00e9rnagas", 0xFFB8FF4A, true, 12);
        b("oroliquido", "Oro L\u00edquido", 0xFFFFC83D, true, 12);
        b("cristal", "Cristal Prisma", 0xFF8FE9FF, true, 12);
        b("portal", "Portal M\u00edstico", 0xFFB04DFF, true, 12);
        b("dragon", "Drag\u00f3n", 0xFFD6202A, true, 12);        CAPES.addAll(BUILTIN);
    }

    public static List<Item> capes() { return CAPES; }

    public static Item find(String id) {
        if (id == null) return null;
        for (Item i : CAPES) if (i.id.equals(id)) return i;
        return null;
    }

    public static String selectedId() { ensureLoaded(); return selected; }

    /** Eleccion tuya (desde el menu): se guarda y se sube al servicio. */
    public static void select(String id) {
        ensureLoaded();
        Item it = find(id);
        if (it != null && it.locked) return;
        selected = id;
        save();
        CosmeticSync.markDirty();
    }

    /** Eleccion que llega del servicio (por ejemplo hecha desde 1.8.9): no se vuelve a subir. */
    static void applyRemote(String id) {
        ensureLoaded();
        String v = "none".equals(id) || id == null || id.isEmpty() ? null : id;
        if ((v == null && selected == null) || (v != null && v.equals(selected))) return;
        selected = v;
        save();
    }

    /** Catalogo nuevo: se reemplazan las capas custom y se marcan las privadas que no son tuyas. */
    static void applyCatalog(List<Def> defs, String me) {
        List<Item> next = new ArrayList<>(BUILTIN);
        String low = me == null ? "" : me.toLowerCase(Locale.ROOT);
        for (Def d : defs) {
            boolean dup = false;
            for (Item i : next) if (i.id.equals(d.id)) { dup = true; break; }
            if (dup) continue;
            Item it = new Item(d.id, d.name, d.color, d.glow, d.frames, true, d.ver);
            it.locked = d.owners != null && !d.owners.contains(low);
            next.add(it);
        }
        CAPES.clear();
        CAPES.addAll(next);
    }

    // ------------------------------------------------------------------ disco

    private static File file() { return new File(SharedDir.dir(), "cosmetics.json"); }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File f = file();
            if (f.isFile()) {
                JsonObject o = JsonParser.parseString(Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("cape") && !o.get("cape").isJsonNull()) {
                    String s = o.get("cape").getAsString();
                    selected = s.isEmpty() || "none".equals(s) ? null : s;
                }
            }
        } catch (Exception ignored) { }
    }

    private static synchronized void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("cape", selected == null ? "none" : selected);
            Files.writeString(file().toPath(), o.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }
}
