package dev.phi.prismlib.api;

import java.util.List;

/** The grid-simulated water a client has right now (a whirlpool, later wakes and waves), for the drawing mod to draw. */
public interface WaterSurfaces {
    List<WaterSurface> surfaces();
}
