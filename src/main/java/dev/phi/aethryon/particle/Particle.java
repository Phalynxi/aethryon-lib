package dev.phi.aethryon.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** One particle. Plain fields: the kind's behaviour and the spawner set them. Positions are world coordinates, velocities blocks per tick. */
public final class Particle {
    public final ParticleKind kind; public final ClientLevel level; public final RandomSource random;
    public double x, y, z, xo, yo, zo, vx, vy, vz;
    public float size, roll, r = 1f, g = 1f, b = 1f, a = 1f;
    public int age, life = 40;
    public TextureAtlasSprite sprite;
    /** Free numbers for the behaviour (a phase, a size, a counter, a direction). */
    public final float[] data = new float[4];
    boolean dead; int light = 0xF000F0, lightAt = -1;

    Particle(ParticleKind kind, ClientLevel level, double x, double y, double z) {
        this.kind = kind; this.level = level; this.random = level.random; this.x = xo = x; this.y = yo = y; this.z = zo = z;
    }

    public void kill() { dead = true; }
    public boolean dead() { return dead; }
    public void colour(int rgb) { r = (rgb >> 16 & 255) / 255f; g = (rgb >> 8 & 255) / 255f; b = (rgb & 255) / 255f; }
    /** Each channel scaled by a random factor in 1 - spread .. 1 + spread, so a cloud of them is not one flat colour. */
    public void tint(int rgb, float spread) {
        float k = 1f - spread + random.nextFloat() * spread * 2f;
        r = Math.min(1f, (rgb >> 16 & 255) / 255f * k); g = Math.min(1f, (rgb >> 8 & 255) / 255f * k); b = Math.min(1f, (rgb & 255) / 255f * k);
    }
    /** 0 at birth rising to 1 over the first {@code in} of its life, falling to 0 over the last {@code out} of it. */
    public float fade(float in, float out) { float t = (float) age / life; return Mth.clamp(Math.min(t / in, (1f - t) / out), 0f, 1f); }
    /** How far through its life, 0 to 1. */
    public float t() { return (float) age / life; }
    /** Move by the velocity. */
    public void move() { x += vx; y += vy; z += vz; }
    public BlockState stateAt(double px, double py, double pz) { return level.getBlockState(BlockPos.containing(px, py, pz)); }
    /** Is the point inside something solid (a block with a collision shape)? */
    public boolean solidAt(double px, double py, double pz) {
        BlockPos bp = BlockPos.containing(px, py, pz); BlockState st = level.getBlockState(bp);
        if (st.getCollisionShape(level, bp).isEmpty()) return false;
        var shape = st.getCollisionShape(level, bp);
        return py < bp.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) && py >= bp.getY() + shape.min(net.minecraft.core.Direction.Axis.Y);
    }
}
