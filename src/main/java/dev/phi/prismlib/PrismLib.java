package dev.phi.prismlib;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Prism Lib: what Phalynxi's mods share. Mods depend on this and not on each other: each one registers what it knows (wind, clouds, wetness, disasters, ...)
 * with {@link dev.phi.prismlib.service.Services} and reads what the others know through {@link dev.phi.prismlib.api.PrismApi}, which falls back to sensible vanilla-based defaults
 * when nobody provides something, so every mod also works alone.
 */
@Mod(PrismLib.ID)
public final class PrismLib {
    public static final String ID = "prismlib";
    public static final Logger LOG = LogUtils.getLogger();

    public PrismLib(IEventBus bus) {
        LOG.info("Prism Lib: ready");
    }
}
