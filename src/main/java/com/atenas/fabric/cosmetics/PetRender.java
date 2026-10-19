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

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Mascotas cosmeticas (version del 1.20.1 con jerarquia de piezas y animacion: patas, cola, cabeza, orejas, alas y salto del conejo): el modelo sale de pets/models.json (piezas de cajas con UV), la mascota te sigue
 * caminando y mira hacia donde va. Solo existen en tu pantalla. Sin subirse al hombro (pendiente).
 * Se dibuja dentro de la CosmeticLayer del jugador, convirtiendo su posicion de mundo al espacio del modelo del jugador.
 */
public final class PetRender {
    private PetRender() {}

    private static final class Piece { String name, parent, role = ""; float[] s = new float[3], o = new float[3], at = new float[3], uv = new float[2]; }
    private static final class Kind { float scale = 1f, tailUp, tailCurl, earRest; boolean hop; List<Piece> pieces = new ArrayList<>(); }
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
                if (k.has("tailUp")) kk.tailUp = k.get("tailUp").getAsFloat();
                if (k.has("tailCurl")) kk.tailCurl = k.get("tailCurl").getAsFloat();
                if (k.has("earRest")) kk.earRest = k.get("earRest").getAsFloat();
                if (k.has("hop")) kk.hop = k.get("hop").getAsBoolean();
                for (JsonElement pe : k.getAsJsonArray("pieces")) {
                    JsonObject po = pe.getAsJsonObject();
                    Piece p = new Piece();
                    p.name = po.get("n").getAsString();
                    p.parent = po.has("p") ? po.get("p").getAsString() : "";
                    p.role = po.has("role") ? po.get("role").getAsString() : "";
                    for (int i = 0; i < 3; i++) { p.s[i] = po.getAsJsonArray("s").get(i).getAsFloat(); p.o[i] = po.getAsJsonArray("o").get(i).getAsFloat(); p.at[i] = po.getAsJsonArray("at").get(i).getAsFloat(); }
                    for (int i = 0; i < 2; i++) p.uv[i] = po.getAsJsonArray("uv").get(i).getAsFloat();
                    kk.pieces.add(p);
                }
                KINDS.put(e.getKey(), kk);
            }
        } catch (Throwable t) { System.err.println("[AtenasClient] mascotas: " + t); }
    }

    private static final class Anim { float amp; }
    private static final Map<Integer, Anim> ANIMS = new HashMap<>();

    /**
     * Dibuja la mascota dentro de la capa del jugador (espacio del modelo: y hacia abajo, suelo a 1.5).
     * Usa la posicion y el giro del RENDER STATE (no los de la entidad): en el menu de cosmeticos el estado lleva el giro de la vista previa
     * y la entidad real ya lo tiene restaurado, por eso antes la mascota salia desplazada o fuera de la caja.
     * preview = true: la mascota se queda fija junto al personaje, mirando al frente, con animacion en reposo.
     */
    public static void render(MatrixStack ms, OrderedRenderCommandQueue queue, int light, int entityId, double ex, double ey, double ez,
                              float bodyYawDeg, boolean preview, float age, String id) {
        load();
        String kn = kindOf == null ? null : kindOf.get(id);
        Kind kind = kn == null ? null : KINDS.get(kn);
        if (kind == null) return;
        if (PETS.size() > 100) { PETS.clear(); ANIMS.clear(); }
        Pet pet = PETS.computeIfAbsent(preview ? -1 : entityId, k -> new Pet());
        Anim an = ANIMS.computeIfAbsent(preview ? -1 : entityId, k -> new Anim());
        long now = System.nanoTime();
        float dt = pet.init ? Math.min(0.1f, (now - pet.last) / 1.0e9f) * 20f : 0f;
        pet.last = now;

        double qx, qz, qy, yawDeg;
        boolean moving = false;
        if (preview) {
            qx = -0.75; qz = -0.25; qy = 1.501; yawDeg = 20.0; // al lado, mirando al frente
            pet.init = true;
        } else {
            double by = Math.toRadians(bodyYawDeg);
            double tx = ex + (Math.sin(by) + Math.cos(by)) * 0.9, tz = ez + (Math.sin(by) - Math.cos(by)) * 0.9;
            if (!pet.init || Math.hypot(pet.x - ex, pet.z - ez) > 14) { pet.x = tx; pet.z = tz; pet.y = ey; pet.hx = -Math.sin(by); pet.hz = Math.cos(by); pet.init = true; }
            double dx = tx - pet.x, dz = tz - pet.z, dist = Math.hypot(dx, dz);
            if (dist > 1.4) {
                double step = Math.min(dist - 1.0, (dist > 4 ? 0.45 : 0.22) * dt);
                pet.x += dx / dist * step; pet.z += dz / dist * step;
                pet.hx = dx / dist; pet.hz = dz / dist;
                moving = step > 0.001;
            } else {
                // en reposo mira hacia el jugador
                double lx = ex - pet.x, lz = ez - pet.z, ll = Math.hypot(lx, lz);
                if (ll > 0.01) { double k = Math.min(1.0, dt * 0.15); pet.hx += (lx / ll - pet.hx) * k; pet.hz += (lz / ll - pet.hz) * k; double hl = Math.hypot(pet.hx, pet.hz); if (hl > 1e-4) { pet.hx /= hl; pet.hz /= hl; } }
            }
            pet.y += (ey - pet.y) * Math.min(1f, dt * 0.3f);
            double sy = pet.y - ey;
            // mundo -> espacio del modelo del jugador (inverso de rotar (180 - yaw), escalar (-1,-1,1) y bajar 1.501)
            double phi = Math.toRadians(180.0 - bodyYawDeg), c = Math.cos(phi), s = -Math.sin(phi);
            double wx = pet.x - ex, wz = pet.z - ez;
            qx = -(wx * c + wz * s); qz = -wx * s + wz * c; qy = -sy + 1.501;
            double hlx = -(pet.hx * c + pet.hz * s), hlz = -pet.hx * s + pet.hz * c;
            yawDeg = Math.toDegrees(Math.atan2(-hlx, -hlz));
        }
        if (moving) pet.walk += dt * 0.9f;
        an.amp += ((moving ? 1f : 0f) - an.amp) * Math.min(1f, dt * 0.25f);
        float amp = an.amp;
        float bob = moving ? Math.abs((float) Math.sin(pet.walk)) * 0.035f : 0f;
        Identifier tex = Identifier.of(Cosmetics.NS, "textures/cosmetics/pets/" + id + ".png");
        final Kind k = kind;
        final float t = age, walk = pet.walk;
        final boolean hop = k.hop;
        ms.push();
        ms.translate(qx, qy - bob - (hop ? Math.abs((float) Math.sin(walk)) * 0.12f * amp : 0f), qz);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) yawDeg));
        float sc = k.scale;
        ms.scale(sc, sc, sc);
        ms.translate(0.0, -1.5, 0.0); // el modelo tiene el suelo en y = 24/16: se escala desde el suelo, no desde el aire
        queue.submitCustom(ms, RenderLayers.entityCutoutNoCull(tex), (e, vc) -> {
            Map<String, Matrix4f> mats = new HashMap<>();
            for (Piece pc : k.pieces) {
                Matrix4f par = pc.parent.isEmpty() ? null : mats.get(pc.parent);
                Matrix4f m = par == null ? new Matrix4f() : new Matrix4f(par);
                m.translate(pc.at[0] / 16f, pc.at[1] / 16f, pc.at[2] / 16f);
                float rx = 0, ry = 0, rz = 0;
                String n = pc.name;
                boolean front = n.contains("FR") || n.contains("FL");
                switch (pc.role) {
                    case "leg1" -> {
                        float ph = (n.startsWith("legFR") || n.startsWith("legRL") || n.startsWith("legR1") ? 0f : (float) Math.PI);
                        if (hop) ph = front ? 0f : 0.5f;
                        rx = (float) Math.sin(walk + ph) * 0.75f * amp * (hop ? 1.2f : 1f);
                    }
                    case "tail", "tailtip" -> {
                        boolean first = pc.parent.equals("body");
                        rx = first ? k.tailUp : k.tailCurl * 0.5f;
                        ry = (float) Math.sin(t * (0.18f + 0.1f * amp) + pc.at[2] * 0.1f) * (0.18f + 0.15f * amp) * (first ? 1f : 0.7f);
                    }
                    case "head" -> { rx = (float) Math.sin(t * 0.07f) * 0.04f; ry = (float) Math.sin(t * 0.045f) * 0.12f; }
                    case "ear", "horn" -> rz = (n.endsWith("L") ? 1f : -1f) * (k.earRest + (float) Math.max(0, Math.sin(t * 0.21f + (n.endsWith("L") ? 0f : 2f)) - 0.85f) * 0.8f);
                    case "wing1" -> rz = (n.contains("L") ? 1f : -1f) * (0.04f + 0.18f * amp * (float) Math.sin(t * 0.9f));
                    default -> { }
                }
                if (rx != 0) m.rotateX(rx);
                if (ry != 0) m.rotateY(ry);
                if (rz != 0) m.rotateZ(rz);
                mats.put(pc.name, m);
                cuboid(vc, e, light, m, pc.o[0], pc.o[1], pc.o[2], pc.s[0], pc.s[1], pc.s[2], pc.uv[0], pc.uv[1]);
            }
        });
        ms.pop();
    }

    private static final Vector3f TP = new Vector3f(), TN = new Vector3f();

    private static void quad(VertexConsumer vc, MatrixStack.Entry e, int light, Matrix4f m, float nx, float ny, float nz, float[] a, float[] b, float[] c, float[] d,
                             float u0, float v0, float u1, float v1) {
        float[][] p = {a, b, c, d};
        float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
        m.transformDirection(nx, ny, nz, TN);
        for (int i = 0; i < 4; i++) {
            m.transformPosition(p[i][0] / 16f, p[i][1] / 16f, p[i][2] / 16f, TP);
            vc.vertex(e, TP.x, TP.y, TP.z).color(255, 255, 255, 255).texture(uv[i][0] / sheetW, uv[i][1] / sheetH).overlay(0).light(light).normal(e, TN.x, TN.y, TN.z);
        }
    }

    private static void cuboid(VertexConsumer vc, MatrixStack.Entry e, int light, Matrix4f m, float x, float y, float z, float w, float h, float d, float u, float v) {
        float x2 = x + w, y2 = y + h, z2 = z + d;
        // frente (z minimo), atras, izquierda, derecha, arriba (y minimo), abajo
        quad(vc, e, light, m, 0, 0, -1, new float[] {x2, y2, z}, new float[] {x, y2, z}, new float[] {x, y, z}, new float[] {x2, y, z}, u + d, v + d, u + d + w, v + d + h);
        quad(vc, e, light, m, 0, 0, 1, new float[] {x, y2, z2}, new float[] {x2, y2, z2}, new float[] {x2, y, z2}, new float[] {x, y, z2}, u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h);
        quad(vc, e, light, m, -1, 0, 0, new float[] {x, y2, z}, new float[] {x, y2, z2}, new float[] {x, y, z2}, new float[] {x, y, z}, u + d + w, v + d, u + 2 * d + w, v + d + h);
        quad(vc, e, light, m, 1, 0, 0, new float[] {x2, y2, z2}, new float[] {x2, y2, z}, new float[] {x2, y, z}, new float[] {x2, y, z2}, u, v + d, u + d, v + d + h);
        quad(vc, e, light, m, 0, -1, 0, new float[] {x2, y, z2}, new float[] {x2, y, z}, new float[] {x, y, z}, new float[] {x, y, z2}, u + d, v, u + d + w, v + d);
        quad(vc, e, light, m, 0, 1, 0, new float[] {x2, y2, z}, new float[] {x2, y2, z2}, new float[] {x, y2, z2}, new float[] {x, y2, z}, u + d + w, v, u + d + 2 * w, v + d);
    }
}
