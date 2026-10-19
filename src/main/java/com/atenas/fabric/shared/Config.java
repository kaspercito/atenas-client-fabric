package com.atenas.fabric.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/** Ajustes simples (true/false) en <.minecraft>/atenas/fabric-config.properties. */
public final class Config {
    private Config() {}

    private static final Properties P = new Properties();
    private static boolean loaded;

    private static File file() { return new File(SharedDir.dir(), "fabric-config.properties"); }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (FileInputStream in = new FileInputStream(file())) { P.load(in); } catch (Exception ignored) { }
    }

    public static synchronized boolean get(String key, boolean def) {
        load();
        String v = P.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    public static synchronized boolean toggle(String key, boolean def) {
        boolean n = !get(key, def);
        P.setProperty(key, Boolean.toString(n));
        try (FileOutputStream out = new FileOutputStream(file())) { P.store(out, "Atenas Client 1.21.11"); } catch (Exception ignored) { }
        return n;
    }
}
