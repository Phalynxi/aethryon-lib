package dev.phi.aethryon.api;

/**
 * Something disturbed the world at a place: a step, a wheel on the ground, the wash of a rotor, a hull moving through, a blast, a splash. Whoever causes it sends one
 * ({@link AethryonApi#disturb}); whoever reacts (snow that is pressed down or blown away, sand and dust that rise, water that ripples, leaves that shake, particles) listens
 * ({@link DisturbanceListener}). The cause and the effect never need to know each other: a wheel from any mod makes tracks in the snow of any other.
 *
 * @param kind     what kind of disturbance
 * @param x,y,z    where, in world coordinates (y: the surface or the point it acts at)
 * @param radius   how far it reaches, in blocks
 * @param energy   how hard, 0 (a touch) to 1 (a vehicle's weight, a rotor at full power); larger values are allowed for blasts
 * @param dx,dy,dz the direction it pushes or moves (a unit vector, or zero when it has none)
 */
public record Disturbance(Kind kind, double x, double y, double z, float radius, float energy, double dx, double dy, double dz) {
    public enum Kind {
        /** A foot or paw on the ground: presses soft ground down a little, makes a print. */
        STEP,
        /** A wheel or a track on the ground: presses a line into soft ground, deeper with weight; energy is how heavy. */
        WHEEL,
        /** A rotor or a thruster blowing on the ground or the water below it: lifts loose things (snow, sand, dust, leaves), pushes water; direction is the way the air goes. */
        WASH,
        /** A body moving through water or loose material (a hull, a heavy fall): pushes it aside. */
        HULL,
        /** A sudden burst: shakes loose snow, throws dust and water. */
        BLAST,
        /** Water thrown onto land, or a drop into water. */
        SPLASH
    }

    public static Disturbance at(Kind kind, double x, double y, double z, float radius, float energy) { return new Disturbance(kind, x, y, z, radius, energy, 0, 0, 0); }
}
