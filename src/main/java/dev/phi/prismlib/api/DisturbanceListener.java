package dev.phi.prismlib.api;

import net.minecraft.world.level.Level;

/** A mod that reacts to disturbances. Called on the side the disturbance was sent on (the server for the world, the client for what is only seen); keep it cheap: wheels send one every tick. */
public interface DisturbanceListener {
    void on(Level level, Disturbance disturbance);
}
