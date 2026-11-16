package dev.phi.prismlib.api;

import net.minecraft.world.level.Level;

/**
 * Where the clouds are: one function of position and time that the server, every client and the shaders work out alike, so everyone sees the same clouds and rain falls only where they are thick.
 * The shaders have the same maths (they cannot call Java), so a model that is not the default must say so through {@link #shaderIdentical()}.
 */
public interface CloudModel {
    /** Cloud cover over a point, 0 (clear) to 1 (thick). */
    float cover(Level level, double x, double z, float partial);
    /** How far the clouds have drifted (x, z) and how far they have changed, for a game time and partial tick. */
    double driftX(long gameTime, float partial);
    double driftZ(long gameTime, float partial);
    float evolve(long gameTime, float partial);
    /** The height the cloud layer starts at, and how tall it can be. */
    float baseHeight();
    float layerHeight();
    /** True when the shaders' own cloud maths gives the same answer (the default model: yes). */
    default boolean shaderIdentical() { return true; }
}
