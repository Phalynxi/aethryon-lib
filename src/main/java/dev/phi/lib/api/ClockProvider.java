package dev.phi.lib.api;

/** The one clock every animation runs on (waves, sway, caustics, funnels): seconds, smooth, never wrapping or jumping. */
public interface ClockProvider {
    float seconds();
}
