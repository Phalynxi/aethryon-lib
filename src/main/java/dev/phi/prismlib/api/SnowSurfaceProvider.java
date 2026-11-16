package dev.phi.prismlib.api;

import net.minecraft.world.level.Level;

/** Where the snow's surface is when something other than the blocks draws it (a smooth sheet over the layers): for whatever must sit on it, such as prints, puffs and spray. */
public interface SnowSurfaceProvider {
    /** The height of the snow surface at a point, or NaN where there is no snow (use the blocks' own height then). */
    float height(Level level, double x, double z);
}
