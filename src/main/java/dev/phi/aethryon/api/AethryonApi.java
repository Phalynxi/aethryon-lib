package dev.phi.aethryon.api;

import dev.phi.aethryon.service.Services;
import dev.phi.aethryon.world.Clouds;
import dev.phi.aethryon.world.Defaults;

/** What the mods ask for. Every call has an answer: the best provider, or a plain default that works with vanilla alone. */
public final class AethryonApi {
    private AethryonApi() {}

    public static ClockProvider clock() { return Services.get(ClockProvider.class, Defaults.CLOCK); }
    public static WindProvider wind() { return Services.get(WindProvider.class, Defaults.WIND); }
    public static CloudModel clouds() { return Services.get(CloudModel.class, Clouds.DEFAULT); }
    public static WetnessProvider wetness() { return Services.get(WetnessProvider.class, Defaults.WETNESS); }
    public static StormProvider storm() { return Services.get(StormProvider.class, Defaults.STORM); }
    public static SeaProvider sea() { return Services.get(SeaProvider.class, Defaults.SEA); }
    public static WaterEvents waterEvents() { return Services.get(WaterEvents.class, Defaults.WATER_EVENTS); }
    /** The first air above the ground at a column, leaves not counted (works on the client). */
    public static int ground(net.minecraft.world.level.Level level, int x, int z) { return Defaults.ground(level, x, z); }
    /** Tell everyone that listens that the world was disturbed (see {@link Disturbance}). */
    public static void disturb(net.minecraft.world.level.Level level, Disturbance d) {
        for (var l : Services.all(DisturbanceListener.class)) {
            try { l.on(level, d); } catch (RuntimeException e) { dev.phi.aethryon.Aethryon.LOG.error("A disturbance listener failed", e); }   // one listener must not stop the others
        }
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
