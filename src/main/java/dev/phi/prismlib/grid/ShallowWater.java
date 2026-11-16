package dev.phi.prismlib.grid;

/**
 * Water as a grid of its own, not the game's. A square window of cells, each holding the height of the surface above its rest level and the velocity of the water there; the
 * shallow-water equations move them: water is pushed down the slope of the surface, carried along by its own flow, loses a little to drag, and the surface rises where the flow
 * converges and falls where it spreads. So waves run out from a disturbance, bounce off solid cells, and a body of water that is made to turn finds the dip in its middle by itself
 * (the pressure that holds a spinning flow together). Whatever wants to disturb it does so through a {@link Forcing}; whatever wants to know what the water does reads it back
 * (height, flow, slope) at any point. Pure maths: no game classes, so the server, the client and the tests run the same thing.
 */
public final class ShallowWater {
    /** Something that pushes the water every step: a spinning vortex, a boat's wake, a rain of drops. */
    public interface Forcing { void apply(ShallowWater g, float dt); }

    public final int n; public final float cell; public double ox, oz;
    /** Surface height above rest, in blocks; flow in blocks per second (x, z). */
    public final float[] eta, u, w;
    public final boolean[] solid;
    /** Mean depth of the water in blocks (sets how fast waves travel: sqrt(g * depth)), gravity in blocks per second squared, drag per second. */
    public float depth = 4f, gravity = 9.8f, drag = 0.12f;
    public Forcing forcing;
    private final float[] u2, w2, e2;

    /** A window of n by n cells of the given size (blocks) centred on (cx, cz). */
    public ShallowWater(int n, float cell, double cx, double cz) {
        this.n = n; this.cell = cell; this.ox = cx - n * cell / 2.0; this.oz = cz - n * cell / 2.0;
        eta = new float[n * n]; u = new float[n * n]; w = new float[n * n]; solid = new boolean[n * n];
        u2 = new float[n * n]; w2 = new float[n * n]; e2 = new float[n * n];
    }

    /** Slide the window so it is centred on (cx, cz), by whole cells: the water that is still in the window keeps moving, what comes into it is still. */
    public void recentre(double cx, double cz) {
        int di = (int) Math.round((cx - n * cell / 2.0 - ox) / cell), dj = (int) Math.round((cz - n * cell / 2.0 - oz) / cell);
        if (di == 0 && dj == 0) return;
        if (Math.abs(di) >= n || Math.abs(dj) >= n) { java.util.Arrays.fill(eta, 0f); java.util.Arrays.fill(u, 0f); java.util.Arrays.fill(w, 0f); }
        else for (float[] a : new float[][] {eta, u, w}) {
            System.arraycopy(a, 0, e2, 0, a.length); java.util.Arrays.fill(a, 0f);
            for (int j = 0; j < n; j++) { int sj = j + dj; if (sj < 0 || sj >= n) continue; for (int i = 0; i < n; i++) { int si = i + di; if (si >= 0 && si < n) a[i + j * n] = e2[si + sj * n]; } }
        }
        ox += di * cell; oz += dj * cell;
    }
    public double worldX(int i) { return ox + (i + 0.5) * cell; }
    public double worldZ(int j) { return oz + (j + 0.5) * cell; }
    public boolean inside(double x, double z) { return x >= ox && z >= oz && x < ox + n * cell && z < oz + n * cell; }

    /** Advance by dt seconds (split into steps small enough to be stable). */
    public void step(float dt) {
        float c = (float) Math.sqrt(gravity * depth);
        int sub = Math.max(1, (int) Math.ceil(c * dt / (0.4f * cell)));
        float h = dt / sub;
        for (int s = 0; s < sub; s++) {
            if (forcing != null) forcing.apply(this, h);
            advect(h);
            float k = gravity * h / (2f * cell), keep = Math.max(0f, 1f - drag * h);
            for (int j = 1; j < n - 1; j++) for (int i = 1; i < n - 1; i++) {   // the surface's slope pushes the water (forward step)
                int c0 = i + j * n;
                if (solid[c0]) { u[c0] = 0f; w[c0] = 0f; continue; }
                float eE = solid[c0 + 1] ? eta[c0] : eta[c0 + 1], eW = solid[c0 - 1] ? eta[c0] : eta[c0 - 1], eS = solid[c0 + n] ? eta[c0] : eta[c0 + n], eN = solid[c0 - n] ? eta[c0] : eta[c0 - n];
                u[c0] = (u[c0] - k * (eE - eW)) * keep; w[c0] = (w[c0] - k * (eS - eN)) * keep;
            }
            float kd = h / (2f * cell);
            for (int j = 1; j < n - 1; j++) for (int i = 1; i < n - 1; i++) {   // and the flow raises or lowers the surface (backward step: uses the new flow)
                int c0 = i + j * n; if (solid[c0]) { eta[c0] = 0f; continue; }
                float uE = solid[c0 + 1] ? 0f : u[c0 + 1], uW = solid[c0 - 1] ? 0f : u[c0 - 1], wS = solid[c0 + n] ? 0f : w[c0 + n], wN = solid[c0 - n] ? 0f : w[c0 - n];
                float d = Math.max(0.5f * depth, depth + eta[c0]);
                eta[c0] -= d * kd * ((uE - uW) + (wS - wN));
            }
            smoothAndSponge(h);
        }
    }

    /** The flow carries itself along (semi-Lagrangian: each cell takes the value from where its water came from). */
    private void advect(float h) {
        float inv = h / cell;
        for (int j = 1; j < n - 1; j++) for (int i = 1; i < n - 1; i++) {
            int c0 = i + j * n;
            float fx = Math.max(0.5f, Math.min(n - 1.5f, i - u[c0] * inv)), fz = Math.max(0.5f, Math.min(n - 1.5f, j - w[c0] * inv));
            int x0 = (int) fx, z0 = (int) fz; float tx = fx - x0, tz = fz - z0; int q = x0 + z0 * n;
            u2[c0] = bil(u, q, tx, tz); w2[c0] = bil(w, q, tx, tz); e2[c0] = bil(eta, q, tx, tz);
        }
        for (int j = 1; j < n - 1; j++) for (int i = 1; i < n - 1; i++) { int c0 = i + j * n; if (solid[c0]) continue; u[c0] = u2[c0]; w[c0] = w2[c0]; eta[c0] = e2[c0]; }
    }
    private float bil(float[] a, int q, float tx, float tz) { return (a[q] * (1 - tx) + a[q + 1] * tx) * (1 - tz) + (a[q + n] * (1 - tx) + a[q + n + 1] * tx) * tz; }

    /** A little diffusion against checkerboard noise, and an absorbing rim so waves leave the window instead of coming back. */
    private void smoothAndSponge(float h) {
        float diff = Math.min(0.15f, 3.0f * h);
        System.arraycopy(eta, 0, e2, 0, eta.length);
        for (int j = 1; j < n - 1; j++) for (int i = 1; i < n - 1; i++) {
            int c0 = i + j * n; if (solid[c0]) continue;
            eta[c0] += diff * 0.25f * (e2[c0 + 1] + e2[c0 - 1] + e2[c0 + n] + e2[c0 - n] - 4f * e2[c0]);
        }
        int band = Math.max(3, n / 10);
        for (int j = 0; j < n; j++) for (int i = 0; i < n; i++) {
            int e = Math.min(Math.min(i, n - 1 - i), Math.min(j, n - 1 - j));
            if (e >= band) continue;
            float s = 1f - (float) e / band, k = Math.max(0f, 1f - (0.5f + 6f * s * s) * h), c0 = i + j * n;
            eta[(int) c0] *= k; u[(int) c0] *= k; w[(int) c0] *= k;
        }
    }

    // ---- reading it back
    /** The surface height above rest at a point (blocks), 0 outside the window. */
    public float height(double x, double z) { return sample(eta, x, z); }
    /** The flow at a point (blocks per second) into out[0] (x), out[1] (z). */
    public void flow(double x, double z, float[] out) { out[0] = sample(u, x, z); out[1] = sample(w, x, z); }
    /** The slope of the surface at a point (blocks of rise per block), east and south, into out. */
    public void slope(double x, double z, float[] out) {
        float d = cell; out[0] = (sample(eta, x + d, z) - sample(eta, x - d, z)) / (2f * d); out[1] = (sample(eta, x, z + d) - sample(eta, x, z - d)) / (2f * d);
    }
    private float sample(float[] a, double x, double z) {
        double fx = (x - ox) / cell - 0.5, fz = (z - oz) / cell - 0.5;
        if (fx < 0 || fz < 0 || fx >= n - 1 || fz >= n - 1) return 0f;
        int x0 = (int) fx, z0 = (int) fz; float tx = (float) (fx - x0), tz = (float) (fz - z0);
        return bil(a, x0 + z0 * n, tx, tz);
    }

    // ---- disturbing it
    /** A drop, a splash or a footstep: pushes the surface down (negative) or up (positive) with a smooth round dent of the given radius. */
    public void impulse(double x, double z, float radius, float amount) {
        int r = (int) Math.ceil(radius / cell);
        int ci = (int) Math.floor((x - ox) / cell), cj = (int) Math.floor((z - oz) / cell);
        for (int j = Math.max(1, cj - r); j <= Math.min(n - 2, cj + r); j++) for (int i = Math.max(1, ci - r); i <= Math.min(n - 2, ci + r); i++) {
            double d = Math.hypot(worldX(i) - x, worldZ(j) - z) / radius; if (d >= 1.0) continue;
            double f = 0.5 + 0.5 * Math.cos(Math.PI * d);
            if (!solid[i + j * n]) eta[i + j * n] += (float) (amount * f);
        }
    }
}
