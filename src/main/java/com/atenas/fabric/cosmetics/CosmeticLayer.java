package com.atenas.fabric.cosmetics;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OrderedRenderCommandQueue;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

/**
 * Capa de render propia: alas (pegadas al cuerpo) y sombreros (pegados a la cabeza). Se registra con
 * LivingEntityFeatureRendererRegistrationCallback solo para jugadores (ver AtenasFabricClient). Misma logica y mismas mallas que el mod 1.20.1.
 */
public final class CosmeticLayer extends FeatureRenderer<EntityRenderState, EntityModel<EntityRenderState>> {
    private static final int FULL = 0xF000F0;
    private final WingMesh mesh = new WingMesh();
    private final WingPlane plane = new WingPlane();
    private final Sink sink = new Sink();

    private static final class State { float air, phase, spd, vy; long last; double lx, ly, lz; boolean init; }
    private final Map<Integer, State> states = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public CosmeticLayer(FeatureRendererContext ctx) { super(ctx); }

    private static final class Sink implements WingMesh.Sink {
        VertexConsumer vc; MatrixStack.Entry e; int light; float nx, ny = 1f, nz;
        @Override public void normal(float x, float y, float z) { nx = x; ny = y; nz = z; }
        @Override public void vertex(float x, float y, float z, float u, float v) {
            vc.vertex(e, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(0).light(light).normal(e, nx, ny, nz);
        }
    }

    private static Identifier tex(String folder, String id) { return Identifier.of(Cosmetics.NS, "textures/cosmetics/" + folder + "/" + id + ".png"); }

    @Override
    public void render(MatrixStack ms, OrderedRenderCommandQueue queue, int light, EntityRenderState state, float limbAngle, float limbDistance) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        Entity en = mc.world.getEntityById(state.id);
        if (!(en instanceof AbstractClientPlayerEntity p) || p.isInvisible()) return;
        if (!(getContextModel() instanceof BipedEntityModel<?> model)) return;
        String hatId = CosmeticSync.partOf(p, "hat"), wingId = CosmeticSync.partOf(p, "wings");
        double d2 = mc.player == null || p == mc.player ? 0.0 : p.squaredDistanceTo(mc.player);
        float age = p.age + (mc.getRenderTickCounter() == null ? 0f : mc.getRenderTickCounter().getTickProgress(false));

        if (wingId != null) {
            Cosmetics.Item item = Cosmetics.find("wings", wingId);
            if (item != null) wings(ms, queue, light, p, item, model, d2, limbAngle, limbDistance);
        }
        String petId = CosmeticSync.partOf(p, "pet");
        if (petId != null && d2 <= HatMesh.MAX_DIST_SQ) PetRender.render(ms, queue, light, p, petId);
        if (hatId != null && d2 <= HatMesh.MAX_DIST_SQ) {
            Cosmetics.Item item = Cosmetics.find("hat", hatId);
            if (item != null) hat(ms, queue, light, item, model, age);
        }
    }

    private void wings(MatrixStack ms, OrderedRenderCommandQueue queue, int light, AbstractClientPlayerEntity p, Cosmetics.Item item,
                       BipedEntityModel<?> model, double d2, float limbSwing, float limbSwingAmount) {
        int segs = WingMesh.segsFor(d2);
        boolean twoSided = WingMesh.twoSidedFor(d2);
        boolean airNow = !p.isOnGround() && !p.isTouchingWater() && !p.hasVehicle();
        if (states.size() > 200) states.clear();
        State st = states.computeIfAbsent(p.getId(), k -> { State n = new State(); n.last = System.nanoTime(); return n; });
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (now - st.last) / 1.0e9F) * 20F;
        st.last = now;
        st.air += ((airNow ? 1F : 0F) - st.air) * Math.min(1F, dt * 0.25F);
        if (!st.init) { st.init = true; st.lx = p.getX(); st.ly = p.getY(); st.lz = p.getZ(); }
        float mvx = (float) (p.getX() - st.lx), mvz = (float) (p.getZ() - st.lz), mvy = (float) (p.getY() - st.ly);
        float kdt = Math.max(dt, 0.05F); mvx /= kdt; mvz /= kdt; mvy /= kdt;
        st.lx = p.getX(); st.ly = p.getY(); st.lz = p.getZ();
        st.spd += (Math.min(1F, (float) Math.sqrt(mvx * mvx + mvz * mvz) * 4F) - st.spd) * Math.min(1F, dt * 0.2F);
        st.vy += (Math.max(-1F, Math.min(1F, mvy * 2.5F)) - st.vy) * Math.min(1F, dt * 0.3F);
        st.phase += dt * (0.35F + st.air * 0.65F + st.spd * 0.25F + Math.abs(st.vy) * 0.5F * st.air);
        float a = st.air;
        float open = (0.85F + (float) Math.sin(st.phase * 0.2F) * 0.07F + (float) Math.sin(st.phase * 0.47F) * 0.025F - st.spd * 0.22F) * (1F - a)
            + (0.50F + (float) Math.sin(st.phase * 0.55F) * 0.32F - st.vy * 0.18F) * a;
        open += (float) Math.sin(limbSwing * 1.2F) * limbSwingAmount * 0.26F * (1F - a);
        WingPlane.Style ws = item.membrane ? WingPlane.styleOf(item.id) : null;
        float time = st.phase;
        if (ws != null) { time = st.phase * ws.speed; open += ws.idle * (float) Math.sin(st.phase * ws.idleFreq) * (1F - 0.7F * a); open *= ws.open; }
        boolean cape = CosmeticSync.capeOf(p) != null;
        if (cape) open *= 0.80F;
        open *= item.openMul;
        mesh.outset = cape ? 1.2F : 0F; mesh.zoff = cape ? 1.8F : 0F;
        plane.outset = mesh.outset; plane.zoff = mesh.zoff;
        if (p.isInSneakingPose()) open = (cape ? 0.9F : 1.15F) * item.openMul;
        float flap = 0.03F + 0.075F * a + 0.11F * st.spd + Math.abs(limbSwingAmount) * 0.03F;
        if (ws != null) flap *= ws.amp;

        final float fOpen = open, fTime = time, fFlap = flap, fPhase = st.phase;
        final boolean fTwo = twoSided;
        final int fLight = item.glow ? FULL : light;
        final int fSegs = segs;
        final double fd2 = d2;
        ms.push();
        model.body.applyTransform(ms);
        queue.submitCustom(ms, RenderLayer.getEntityCutoutNoCull(tex("wings", item.id)), (e, vc) -> {
            sink.vc = vc; sink.e = e; sink.light = fLight;
            if (item.membrane) plane.emit(sink, fOpen, fTime, fFlap, fTwo, WingPlane.gridFor(fd2), 0.0625F);
            else mesh.emit(sink, fOpen, fPhase, fFlap, fSegs, fTwo, 0.0625F);
            sink.vc = null;
        });
        ms.pop();
    }

    private void hat(MatrixStack ms, OrderedRenderCommandQueue queue, int light, Cosmetics.Item item, BipedEntityModel<?> model, float age) {
        HatMesh m = HatMesh.get(item.id);
        if (m == null) return;
        Identifier t = tex("hats", item.id);
        ms.push();
        model.head.applyTransform(ms);
        if ("halo".equals(item.id)) ms.translate(0.0, Math.sin(age * 0.1f) * 0.02, 0.0);
        queue.submitCustom(ms, RenderLayer.getEntityCutoutNoCull(t), (e, vc) -> {
            for (int g = 0; g < m.groups(); g++) if ((m.flags[g] & HatMesh.EMIT) == 0) m.emit(g, vc, e, light, 1f);
        });
        boolean any = false;
        for (int f : m.flags) if ((f & HatMesh.EMIT) != 0) any = true;
        if (any) {
            queue.submitCustom(ms, RenderLayer.getEyes(t), (e, vc) -> {
                for (int g = 0; g < m.groups(); g++) {
                    int fl = m.flags[g];
                    if ((fl & HatMesh.EMIT) == 0) continue;
                    float al = 1f;
                    if ((fl & HatMesh.PULSE) != 0) al = 0.62f + 0.38f * (float) Math.sin(age * 0.13f + g * 1.7f);
                    if ((fl & HatMesh.SOFT) != 0) al *= 0.7f;
                    m.emit(g, vc, e, FULL, al);
                }
            });
        }
        ms.pop();
    }
}
