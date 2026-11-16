package dev.phi.prismlib.api;

/**
 * A regional storm as it is right now: a place (world coordinates), how far it reaches, how strong it is at the middle (0 to 1) and which way it drifts (blocks per tick).
 * Under it the sky closes and it pours; away from it the weather is whatever it is.
 */
public record StormCell(double x, double z, float radius, float level, double vx, double vz) {
    /** How strongly the cell is over a point: its level in the middle, falling off smoothly to nothing at its radius (a wide core, a soft edge). */
    public float at(double px, double pz) {
        double d = Math.hypot(px - x, pz - z) / radius;
        if (d >= 1.0) return 0f;
        double t = Math.max(0.0, (d - 0.3) / 0.7);
        return level * (1f - (float) (t * t * t * (t * (t * 6 - 15) + 10)));
    }
}
