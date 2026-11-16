package dev.phi.prismlib.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.phi.prismlib.PrismLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Prism Lib's own particle engine: the pool, the tick and the drawing. Particles are made with {@link #spawn}, simulated every client tick by their kind's behaviour and drawn
 * interpolated between ticks, all in one batch from the particle atlas right after the world's particles stage. Light comes from the world at the particle's block.
 */
@EventBusSubscriber(modid = PrismLib.ID, value = Dist.CLIENT)
public final class Particles {
    private Particles() {}
    /** Most particles alive at once: new ones are refused beyond it (the oldest are not culled: they are nearly done anyway). */
    public static final int MAX = 24_000;
    private static final List<Particle> POOL = new ArrayList<>(4096);
    private static final double RANGE2 = 144.0 * 144.0;

    /** Make a particle; {@code init} sets what is not the default (velocity, size, life, colour, alpha, sprite). Returns null when the pool is full or the kind has no sprites yet. */
    public static Particle spawn(ParticleKind kind, net.minecraft.world.level.Level level, double x, double y, double z, Consumer<Particle> init) {
        if (!(level instanceof ClientLevel cl) || POOL.size() >= MAX || !kind.ready()) return null;
        Particle p = new Particle(kind, cl, x, y, z);
        p.sprite = kind.pick(cl.random);
        init.accept(p);
        POOL.add(p);
        return p;
    }

    public static int count() { return POOL.size(); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { POOL.clear(); return; }
        if (mc.isPaused()) return;
        int n = POOL.size();
        for (int i = 0; i < n; i++) {
            Particle p = POOL.get(i);
            if (p.dead || p.level != mc.level) { p.dead = true; continue; }
            p.xo = p.x; p.yo = p.y; p.zo = p.z;
            if (p.age++ >= p.life) { p.dead = true; continue; }
            try { p.kind.behaviour.tick(p); }
            catch (RuntimeException ex) { p.dead = true; PrismLib.LOG.error("A particle behaviour failed; that particle is dropped", ex); }
        }
        POOL.removeIf(p -> p.dead);
    }

    @SubscribeEvent
    public static void left(ClientPlayerNetworkEvent.LoggingOut e) { POOL.clear(); }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || POOL.isEmpty()) return;
        var mc = Minecraft.getInstance(); ClientLevel level = mc.level; if (level == null) return;
        var cam = e.getCamera(); var cp = cam.getPosition(); float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        Quaternionf rot = cam.rotation(); long frame = mc.level.getGameTime();
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.depthMask(false);
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        var bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        int drawn = 0; Vector3f v = new Vector3f(); Quaternionf q = new Quaternionf();
        for (int i = 0, n = POOL.size(); i < n; i++) {
            Particle p = POOL.get(i);
            if (p.dead || p.sprite == null || p.a <= 0.003f) continue;
            double dx = p.x - cp.x, dy = p.y - cp.y, dz = p.z - cp.z;
            if (dx * dx + dy * dy + dz * dz > RANGE2) continue;
            float px = (float) (Mth.lerp(pt, p.xo, p.x) - cp.x), py = (float) (Mth.lerp(pt, p.yo, p.y) - cp.y), pz = (float) (Mth.lerp(pt, p.zo, p.z) - cp.z);
            if ((frame + i) % 8 != p.lightAt % 8 || p.lightAt < 0) { p.light = LevelRenderer.getLightColor(level, BlockPos.containing(p.x, p.y, p.z)); p.lightAt = (int) ((frame + i) & 0x7fffffff); }
            float u0 = p.sprite.getU0(), u1 = p.sprite.getU1(), v0 = p.sprite.getV0(), v1 = p.sprite.getV1(), s = p.size;
            if (p.kind.shape == ParticleKind.Shape.FLAT) {   // counter-clockwise seen from above, so the face looks up
                float cs = Mth.cos(p.roll) * s, sn = Mth.sin(p.roll) * s;
                bb.addVertex(px - cs - sn, py, pz - sn + cs).setUv(u1, v1).setColor(p.r, p.g, p.b, p.a).setLight(p.light);
                bb.addVertex(px + cs - sn, py, pz + sn + cs).setUv(u0, v1).setColor(p.r, p.g, p.b, p.a).setLight(p.light);
                bb.addVertex(px + cs + sn, py, pz + sn - cs).setUv(u0, v0).setColor(p.r, p.g, p.b, p.a).setLight(p.light);
                bb.addVertex(px - cs + sn, py, pz - sn - cs).setUv(u1, v0).setColor(p.r, p.g, p.b, p.a).setLight(p.light);
            } else {
                q.set(rot); if (p.roll != 0f) q.rotateZ(p.roll);
                corner(bb, q, v, -1, -1, s, px, py, pz, u1, v1, p); corner(bb, q, v, -1, 1, s, px, py, pz, u1, v0, p);
                corner(bb, q, v, 1, 1, s, px, py, pz, u0, v0, p); corner(bb, q, v, 1, -1, s, px, py, pz, u0, v1, p);
            }
            drawn++;
        }
        if (drawn > 0) BufferUploader.drawWithShader(bb.buildOrThrow());
        mc.gameRenderer.lightTexture().turnOffLightLayer();
        RenderSystem.depthMask(true); RenderSystem.disableBlend();
    }

    private static void corner(com.mojang.blaze3d.vertex.BufferBuilder bb, Quaternionf q, Vector3f v, float cx, float cy, float s, float px, float py, float pz, float u, float vv, Particle p) {
        v.set(cx, cy, 0f).rotate(q).mul(s).add(px, py, pz);
        bb.addVertex(v.x, v.y, v.z).setUv(u, vv).setColor(p.r, p.g, p.b, p.a).setLight(p.light);
    }
}
