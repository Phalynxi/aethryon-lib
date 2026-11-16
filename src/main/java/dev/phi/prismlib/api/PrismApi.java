package dev.phi.prismlib.api;

import dev.phi.prismlib.service.Services;
import dev.phi.prismlib.world.Clouds;
import dev.phi.prismlib.world.Defaults;

/** What the mods ask for. Every call has an answer: the best provider, or a plain default that works with vanilla alone. */
public final class PrismApi {
    private PrismApi() {}

    public static ClockProvider clock() { return Services.get(ClockProvider.class, Defaults.CLOCK); }
    public static WindProvider wind() { return Services.get(WindProvider.class, Defaults.WIND); }
    public static CloudModel clouds() { return Services.get(CloudModel.class, Clouds.DEFAULT); }
    public static WetnessProvider wetness() { return Services.get(WetnessProvider.class, Defaults.WETNESS); }
    public static StormProvider storm() { return Services.get(StormProvider.class, Defaults.STORM); }
    /** The rain at a place: vanilla's rain level, or the downpour under a storm cell (cells rain even when the weather is clear). */
    public static float rainAt(net.minecraft.world.level.Level level, double x, double z) {
        float best = 0f;
        for (var c : storm().cells(level, x, z, 0)) best = Math.max(best, c.at(x, z));
        return Math.max(level.getRainLevel(1f), Math.min(1f, 1.4f * best));
    }
    /** The snow surface's height at a point, or NaN: ask before placing something on snow. */
    public static float snowHeight(net.minecraft.world.level.Level level, double x, double z) {
        var all = Services.all(SnowSurfaceProvider.class);
        for (var p : all) { float h = p.height(level, x, z); if (!Float.isNaN(h)) return h; }
        return Float.NaN;
    }
    public static SeaProvider sea() { return Services.get(SeaProvider.class, Defaults.SEA); }
    public static WaterEvents waterEvents() { return Services.get(WaterEvents.class, Defaults.WATER_EVENTS); }
    /** The first air above the ground at a column, leaves not counted (works on the client). */
    public static int ground(net.minecraft.world.level.Level level, int x, int z) { return Defaults.ground(level, x, z); }
    /** Tell everyone that listens that the world was disturbed (see {@link Disturbance}). */
    public static void disturb(net.minecraft.world.level.Level level, Disturbance d) {
        for (var l : Services.all(DisturbanceListener.class)) {
            try { l.on(level, d); } catch (RuntimeException e) { dev.phi.prismlib.PrismLib.LOG.error("A disturbance listener failed", e); }   // one listener must not stop the others
        }
    }

    /** The wave field of the water around the camera, or null. */
    public static WaveField waveField() { return Services.get(WaveField.class, null); }
    /** Every mod that simulates water on a grid contributes: the lists are put together. */
    public static java.util.List<WaterSurface> waterSurfaces() {
        var all = Services.all(WaterSurfaces.class);
        if (all.isEmpty()) return java.util.List.of();
        if (all.size() == 1) return all.get(0).surfaces();
        var out = new java.util.ArrayList<WaterSurface>(); for (var v : all) out.addAll(v.surfaces());
        return out;
    }
    /** Every mod that runs disasters contributes: the lists are put together. */
    public static java.util.List<Funnel> funnels() {
        var all = Services.all(DisasterView.class);
        if (all.isEmpty()) return java.util.List.of();
        if (all.size() == 1) return all.get(0).funnels();
        var out = new java.util.ArrayList<Funnel>(); for (var v : all) out.addAll(v.funnels());
        return out;
    }
}
