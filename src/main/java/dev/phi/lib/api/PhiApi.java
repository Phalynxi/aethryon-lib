package dev.phi.lib.api;

import dev.phi.lib.service.Services;
import dev.phi.lib.world.Clouds;
import dev.phi.lib.world.Defaults;

/** What the mods ask for. Every call has an answer: the best provider, or a plain default that works with vanilla alone. */
public final class PhiApi {
    private PhiApi() {}

    public static ClockProvider clock() { return Services.get(ClockProvider.class, Defaults.CLOCK); }
    public static WindProvider wind() { return Services.get(WindProvider.class, Defaults.WIND); }
    public static CloudModel clouds() { return Services.get(CloudModel.class, Clouds.DEFAULT); }
    public static WetnessProvider wetness() { return Services.get(WetnessProvider.class, Defaults.WETNESS); }
    /** Every mod that runs disasters contributes: the lists are put together. */
    public static java.util.List<Funnel> funnels() {
        var all = Services.all(DisasterView.class);
        if (all.isEmpty()) return java.util.List.of();
        if (all.size() == 1) return all.get(0).funnels();
        var out = new java.util.ArrayList<Funnel>(); for (var v : all) out.addAll(v.funnels());
        return out;
    }
}
