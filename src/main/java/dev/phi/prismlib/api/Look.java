package dev.phi.prismlib.api;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The seam between the mods that run the world and the mod that draws it (a shader look such as Alterra Illuminate). The drawing mod sets these on the client when it loads; until it does they
 * stay at defaults that mean "nothing draws this", and the mods that run the world draw their own plain versions (vanilla particles and so on).
 */
public final class Look {
    private Look() {}
    /** Is a shader drawing the water (then ripples, rain rings and splashes are drawn by it)? */
    public static volatile BooleanSupplier shaderWater = () -> false;
    /** Does something draw its own clouds (then vanilla's are left out)? */
    public static volatile BooleanSupplier ownClouds = () -> false;
    /** Precise water amounts from the server's simulation, for a smooth surface. */
    public static volatile Consumer<WaterAmounts> waterAmounts = w -> {};
    /** How much rain water has gathered at a block column (0 dry to 1 a full puddle), as the look worked it out. */
    public static volatile BiFunction<Integer, Integer, Float> puddleAt = (x, z) -> 0f;
    /** Water thrown onto land (x, z, radius in blocks, amount in blocks of water): the look wets the ground around it. */
    public static volatile Consumer<double[]> splashOnLand = a -> {};
}
