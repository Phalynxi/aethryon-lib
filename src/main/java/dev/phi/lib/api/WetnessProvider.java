package dev.phi.lib.api;

import net.minecraft.world.entity.player.Player;

/** How wet the world is around a player, 0 (dry) to 1 (soaked): it rises while it rains and dries slowly afterwards. */
public interface WetnessProvider {
    float of(Player player);
}
