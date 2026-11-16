package dev.phi.prismlib.world;

import dev.phi.prismlib.api.SeaProvider;
import dev.phi.prismlib.api.StormProvider;
import dev.phi.prismlib.api.WaterEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import dev.phi.prismlib.api.ClockProvider;
import dev.phi.prismlib.api.WetnessProvider;
import dev.phi.prismlib.api.WindProvider;
import net.minecraft.world.phys.Vec3;

/** What the services answer when no mod provides them: all of it works with vanilla alone. */
public final class Defaults {
    private Defaults() {}

    private static final long START = System.nanoTime();
    /** Seconds since the game started: smooth, never wraps. */
    public static final ClockProvider CLOCK = () -> (float) ((System.nanoTime() - START) / 1.0e9);

    /** A breeze that slowly turns, stronger in rain and thunder. */
    public static final WindProvider WIND = (level, x, z) -> {
        double t = level.getGameTime() * 0.0009, a = 1.2 * Math.sin(t) + 0.8 * Math.sin(t * 2.3 + 1.0) + 0.0007 * x - 0.0005 * z;
        double len = 0.22 + 0.45 * level.getRainLevel(1f) + 0.5 * level.getThunderLevel(1f);
        return new Vec3(Math.cos(a) * len, 0, Math.sin(a) * len);
    };

    /** Wetness from vanilla's rain level alone: it follows the rain with a lag the caller smooths (the default has no memory of its own). */
    public static final WetnessProvider WETNESS = p -> Math.min(1f, p.level().getRainLevel(1f) * (p.level().canSeeSky(p.blockPosition()) ? 1f : 0.3f));

    /** No storm beyond vanilla's. */
    public static final StormProvider STORM = new StormProvider() {
        public float heavy(Level level) { return 0f; }
        public java.util.List<dev.phi.prismlib.api.StormCell> cells(Level level, double x, double z, double range) { return java.util.List.of(); }
        public float fog() { return 0f; }
        public float drying(Level level, BlockPos pos) { return Math.max(0.2f, Math.min(2.5f, 0.35f + (level.getBiome(pos).value().getBaseTemperature() * 20f - 4f) / 20f)); }
        public float serverWet() { return Float.NaN; }
    };

    /** No swells and no surge: a calm sea. */
    public static final SeaProvider SEA = new SeaProvider() {
        public float open(Level level, int x, int z, BlockPos.MutableBlockPos scratch) { return level.hasChunk(x >> 4, z >> 4) ? 0f : -1f; }
        public boolean surging() { return false; }
        public float surge(double x, double z) { return 0f; }
    };

    /** Nothing happens on the water. */
    public static final WaterEvents WATER_EVENTS = new WaterEvents() {
        public int foam(float[] out, Vec3 camera) { return 0; }
        public int ripples(float[] out, Vec3 camera) { return 0; }
        public int shipBoxes(float[] out, Vec3 camera) { return 0; }
        public int hulls(float[] out, Vec3 camera) { return 0; }
        public boolean shipsHandledElsewhere() { return false; }
    };

    /** The first air above the ground at a column, leaves not counted. Works on the client too: the client keeps only the MOTION_BLOCKING and WORLD_SURFACE height maps
     *  (MOTION_BLOCKING_NO_LEAVES there gives the bottom of the world). */
    public static int ground(Level l, int x, int z) {
        int h = l.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        var p = new BlockPos.MutableBlockPos(x, h - 1, z);
        for (int i = 0; i < 48 && h > l.getMinBuildHeight() && l.getBlockState(p).is(BlockTags.LEAVES); i++) { h--; p.move(0, -1, 0); }
        return h;
    }
}
