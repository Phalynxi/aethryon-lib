package dev.phi.aethryon.api;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The wind at a place: a horizontal vector, length 0 (calm) to about 1.5 (a gale), the way the air moves. */
public interface WindProvider {
    Vec3 at(Level level, double x, double z);
}
