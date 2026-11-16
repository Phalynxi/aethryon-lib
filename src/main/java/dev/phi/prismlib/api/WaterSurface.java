package dev.phi.prismlib.api;

import dev.phi.prismlib.grid.ShallowWater;

/**
 * Water that is simulated on a grid of its own and drawn as its own surface instead of the game's water blocks: the grid, the height of its resting surface (world y), and the radius of
 * the disc around its middle that it stands in for (the water blocks' own surface is not drawn inside it).
 */
public record WaterSurface(ShallowWater grid, double y, float radius) {}
