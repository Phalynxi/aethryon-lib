package dev.phi.prismlib.api;

/** A disaster that has a place in the world and is drawn as a funnel or a swirl: a tornado, a dust devil, a whirlpool. Positions are world coordinates. */
public record Funnel(Type type, double x, double y, double z, float radius, float topRadius, float height, float strength, float spin) {
    public enum Type { TORNADO, DUST_DEVIL, WHIRLPOOL }
}
