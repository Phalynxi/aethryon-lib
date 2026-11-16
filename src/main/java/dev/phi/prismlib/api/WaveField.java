package dev.phi.prismlib.api;

import dev.phi.prismlib.grid.ShallowWater;

/** The grid water around the camera that ordinary water is stirred in (rain, swimmers, boats, splashes): the grid, and whether anything is moving in it right now. */
public interface WaveField {
    ShallowWater grid();
    boolean active();
}
