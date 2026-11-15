package dev.phi.lib.world;

import dev.phi.lib.api.ClockProvider;
import dev.phi.lib.api.WetnessProvider;
import dev.phi.lib.api.WindProvider;
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
}
