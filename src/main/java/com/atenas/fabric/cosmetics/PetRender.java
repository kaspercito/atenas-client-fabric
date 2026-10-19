package com.atenas.fabric.cosmetics;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Mascotas cosmeticas (version simplificada del 1.20.1): el modelo sale de pets/models.json (piezas de cajas con UV), la mascota te sigue
 * caminando y mira hacia donde va. Solo existen en tu pantalla. Sin animacion de patas/cola ni subirse al hombro (pendiente).
 * Se dibuja dentro de la CosmeticLayer del jugador, convirtiendo su posicion de mundo al espacio del modelo del jugador.
 */
public final class PetRender {
    private PetRender() {}

    private static final class Piece { String name, parent; float[] s = new float[3], o = new float[3], at = new float[3], uv = new float[2]; }
    private static final class Kind { float scale = 1f; List<Piece> pieces = new ArrayList<>(); }
    private static final class Pet { double x, z, y; double hx, hz = -1; float walk; long last; boolean init; }

    private static Map<String, String> kindOf;
    private static final Map<String, Kind> KINDS = new HashMap<>();
    private static float sheetW = 128, sheetH = 64;
    private static boolean loaded;
    private static final Map<Integer, Pet> PETS = new HashMap<>();

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        kindOf = new HashMap<>();
        try (InputStream in = PetRender.class.getResourceAsStream("/assets/" + Cosmetics.NS + "/pets/models.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray sh = root.getAsJsonArray("sheet");
            sheetW = sh.get(0).getAsFloat(); sheetH = sh.get(1).getAsFloat();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("pets").entrySet()) kindOf.put(e.getKey(), e.getValue().getAsString());
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("kinds").entrySet()) {
                JsonObject k = e.getValue().getAsJsonObject();
                Kind kk = new Kind();
                if (k.has("scale")) kk.scale = k.get("scale").getAsFloat();
                for (JsonElement pe : k.getAsJsonArray("pieces")) {
                    JsonObject po = pe.getAsJsonObject();
                    Piece p = new Piece();
                    p.name = po.get("n").getAsString();
                    p.parent = po.has("p") ? po.get("p").getAsString() : "";
                    for (int i = 0; i < 3; i++) { p.s[i] = po.getAsJsonArray("s").get(i).getAsFloat(); p.o[i] = po.getAsJsonArray("o").get(i).getAsFloat(); p.at[i] = po.getAsJsonArray("at").get(i).getAsFloat(); }
                    for (int i = 0; i < 2; i++) p.uv[i] = po.getAsJsonArray("uv").get(i).getAsFloat();
                    kk.pieces.add(p);
                }
                KINDS.put(e.getKey(), kk);
            }
        } catch (Throwable t) { System.err.println("[AtenasClient] mascotas: " + t); }
    }

    public static void render(MatrixStack ms, OrderedRenderCommandQueue queue, int light, AbstractClientPlayerEntity p, String id) {
        load();
        String kn = kindOf == null ? null : kindOf.get(id);
        Kind kind = kn == null ? null : KINDS.get(kn);
        if (kind == null) return;
        if (PETS.size() > 100) PETS.clear();
        Pet pet = PETS.computeIfAbsent(p.getId(), k -> new Pet());
        long now = System.nanoTime();
        float dt = pet.init ? Math.min(0.1f, (now - pet.last) / 1.0e9f) * 20f : 0f;
        pet.last = now;

        double by = Math.toRadians(p.getBodyYaw());
        // objetivo: detras y a un lado del jugador
        double tx = p.getX() + (Math.sin(by) + Math.cos(by)) * 0.9, tz = p.getZ() + (Math.sin(by) - Math.cos(by)) * 0.9;
        if (!pet.init || Math.hypot(pet.x - p.getX(), pet.z - p.getZ()) > 14) { pet.x = tx; pet.z = tz; pet.y = p.getY(); pet.init = true; }
        double dx = tx - pet.x, dz = tz - pet.z, dist = Math.hypot(dx, dz);
        boolean moving = false;
        if (dist > 1.4) {
            double step = Math.min(dist - 1.0, (dist > 4 ? 0.45 : 0.22) * dt);
            pet.x += dx / dist * step; pet.z += dz / dist * step;
            pet.hx = dx / dist; pet.hz = dz / dist;
            moving = step > 0.001;
        }
        pet.y += (p.getY() - pet.y) * Math.min(1f, dt * 0.3f);
        if (moving) pet.walk += dt * 0.9f;
        double sy = pet.y - p.getY();

        // mundo -> espacio del modelo del jugador (inverso de rotar (180 - yaw), escalar (-1,-1,1) y bajar 1.501)
        double th = -Math.toRadians(180.0 - p.getBodyYaw()), c = Math.cos(th), s = Math.sin(th);
        double wx = pet.x - p.getX(), wz = pet.z - p.getZ();
        double qx = -(wx * c + wz * s), qz = -wx * s + wz * c, qy = -sy + 1.501;
        double hlx = -(pet.hx * c + pet.hz * s), hlz = -pet.hx * s + pet.hz * c;
        float yawDeg = (float) Math.toDegrees(Math.atan2(-hlx, -hlz));

        float bob = moving ? Math.abs((float) Math.sin(pet.walk)) * 0.035f : 0f;
        Identifier tex = Identifier.of(Cosmetics.NS, "textures/cosmetics/pets/" + id + ".png");
        final Kind k = kind;
        ms.push();
        ms.translate(qx, qy - 1.5 - bob, qz);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
        float sc = k.scale;
        ms.scale(sc, sc, sc);
        queue.submitCustom(ms, RenderLayers.entityCutoutNoCull(tex), (e, vc) -> {
            Map<String, float[]> pivots = new HashMap<>();
            for (Piece pc : k.pieces) {
                float[] par = pc.parent.isEmpty() ? new float[3] : pivots.getOrDefault(pc.parent, new float[3]);
                float px = par[0] + pc.at[0], py = par[1] + pc.at[1], pz = par[2] + pc.at[2];
                pivots.put(pc.name, new float[] {px, py, pz});
                cuboid(vc, e, light, px + pc.o[0], py + pc.o[1], pz + pc.o[2], pc.s[0], pc.s[1], pc.s[2], pc.uv[0], pc.uv[1]);
            }
        });
        ms.pop();
    }

    private static void quad(VertexConsumer vc, MatrixStack.Entry e, int light, float nx, float ny, float nz, float[] a, float[] b, float[] c, float[] d,
                             float u0, float v0, float u1, float v1) {
        float[][] p = {a, b, c, d};
        float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
        for (int i = 0; i < 4; i++) {
            vc.vertex(e, p[i][0] / 16f, p[i][1] / 16f, p[i][2] / 16f).color(255, 255, 255, 255).texture(uv[i][0] / sheetW, uv[i][1] / sheetH).overlay(0).light(light).normal(e, nx, ny, nz);
        }
    }

    private static void cuboid(VertexConsumer vc, MatrixStack.Entry e, int light, float x, float y, float z, float w, float h, float d, float u, float v) {
        float x2 = x + w, y2 = y + h, z2 = z + d;
        // frente (z minimo), atras, izquierda, derecha, arriba (y minimo), abajo
        quad(vc, e, light, 0, 0, -1, new float[] {x2, y2, z}, new float[] {x, y2, z}, new float[] {x, y, z}, new float[] {x2, y, z}, u + d, v + d, u + d + w, v + d + h);
        quad(vc, e, light, 0, 0, 1, new float[] {x, y2, z2}, new float[] {x2, y2, z2}, new float[] {x2, y, z2}, new float[] {x, y, z2}, u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h);
        quad(vc, e, light, -1, 0, 0, new float[] {x, y2, z}, new float[] {x, y2, z2}, new float[] {x, y, z2}, new float[] {x, y, z}, u + d + w, v + d, u + 2 * d + w, v + d + h);
        quad(vc, e, light, 1, 0, 0, new float[] {x2, y2, z2}, new float[] {x2, y2, z}, new float[] {x2, y, z}, new float[] {x2, y, z2}, u, v + d, u + d, v + d + h);
        quad(vc, e, light, 0, -1, 0, new float[] {x2, y, z2}, new float[] {x2, y, z}, new float[] {x, y, z}, new float[] {x, y, z2}, u + d, v, u + d + w, v + d);
        quad(vc, e, light, 0, 1, 0, new float[] {x2, y2, z}, new float[] {x2, y2, z2}, new float[] {x, y2, z2}, new float[] {x, y2, z}, u + d + w, v, u + d + 2 * w, v + d);
    }
}
