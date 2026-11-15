package dev.phi.aethryon.api;

import net.minecraft.world.phys.Vec3;

/**
 * What is happening on the water, for the water to be drawn with: rings from splashes and rain, foam from breaking waves and wakes, the boxes and hulls of ships. Each snapshot
 * fills the array for the camera (positions relative to the camera, as the shaders want them) and returns how many entries it wrote.
 */
public interface WaterEvents {
    int FOAM_MAX = 40, RIPPLES_MAX = 48, SHIP_BOXES_MAX = 8, HULLS_MAX = 4;
    /** Foam patches: 4 floats each. */
    int foam(float[] out, Vec3 camera);
    /** Rings: 4 floats each (x, z relative to the camera, age in seconds, strength). */
    int ripples(float[] out, Vec3 camera);
    /** Ships as boxes the sea is kept out of: 8 floats each. */
    int shipBoxes(float[] out, Vec3 camera);
    /** Ships as oriented hulls: 16 floats each. */
    int hulls(float[] out, Vec3 camera);
    /** True when another mod (Create: Deep Seas) keeps the sea out of its ships itself, so hulls and boxes are not wanted. */
    boolean shipsHandledElsewhere();
}
