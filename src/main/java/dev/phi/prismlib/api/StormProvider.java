package dev.phi.prismlib.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** What the storms and the weather around the player are doing beyond vanilla's rain and thunder levels, as seen on the client. */
public interface StormProvider {
    /** How heavy the storm is, 0 (none) to 1 (a hurricane or the like); more than vanilla's thunder level can say. */
    float heavy(Level level);
    /** The storm cells within {@code range} blocks of a point (their edge counted), with their positions for now. */
    java.util.List<StormCell> cells(Level level, double x, double z, double range);
    /** How far a storm's fog (sandstorm, blizzard, fog, ash) closes in the view: 0 clear, near 1 in the thick of it. */
    float fog();
    /** How fast wet things dry at a place: about 0.2 in the cold, 1 in mild weather, up to 2.5 in the heat. */
    float drying(Level level, BlockPos pos);
    /** How wet the player is as the server counted it, 0 to 1, or NaN when the server has not said anything lately (then the caller estimates). */
    float serverWet();
}
