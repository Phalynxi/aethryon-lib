package dev.phi.aethryon.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The state of the sea: how open it is (how big its swells can grow) and any surge (a tsunami, a storm tide) running over it. */
public interface SeaProvider {
    /** How open the sea is at a column: 0 (shallow, sheltered) to 1 (deep open ocean), or -1 when the chunk is not loaded. */
    float open(Level level, int x, int z, BlockPos.MutableBlockPos scratch);
    /** Is there any surge to draw? */
    boolean surging();
    /** The surge at a point, in blocks above the normal surface. */
    float surge(double x, double z);
}
