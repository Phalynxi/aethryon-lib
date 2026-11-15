package dev.phi.aethryon.particle;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

/**
 * A kind of particle: how it is drawn (a billboard that faces the camera, or a square lying flat), what its sprites are, and how it behaves each tick.
 * A mod makes its kinds once; Aethryon owns every particle's simulation and drawing (nothing goes through Minecraft's particle engine).
 * The sprites come from a particle description file ({@code assets/<mod>/particles/<name>.json}) registered through Minecraft's loader, which only stitches the pictures into the particle atlas:
 * hand the set it gives you to {@link #sprites}.
 */
public final class ParticleKind {
    public enum Shape { BILLBOARD, FLAT }
    /** What a particle does each tick (after its age went up by one and its old position was kept); it moves itself, see {@link Particle#move()}. */
    public interface Behaviour { void tick(Particle p); }

    public final Shape shape; public final Behaviour behaviour;
    private volatile SpriteSet sprites;

    public ParticleKind(Shape shape, Behaviour behaviour) { this.shape = shape; this.behaviour = behaviour; }

    /** Set from the particle provider registration when the atlas has been stitched (again after every resource reload). */
    public void sprites(SpriteSet set) { sprites = set; }
    public boolean ready() { return sprites != null; }
    /** A sprite picked at random, one by age (for animations), or by index of a set that has {@code max} sprites. */
    public TextureAtlasSprite pick(RandomSource r) { var s = sprites; return s == null ? null : s.get(r); }
    public TextureAtlasSprite sprite(int age, int lifetime) { var s = sprites; return s == null ? null : s.get(age, lifetime); }
}
