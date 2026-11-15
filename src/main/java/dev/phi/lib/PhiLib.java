package dev.phi.lib;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Phi Lib: what Phalynxi's mods share. Mods depend on this and not on each other: each one registers what it knows (wind, clouds, wetness, disasters, ...)
 * with {@link dev.phi.lib.service.Services} and reads what the others know through {@link dev.phi.lib.api.PhiApi}, which falls back to sensible vanilla-based defaults
 * when nobody provides something, so every mod also works alone.
 */
@Mod(PhiLib.ID)
public final class PhiLib {
    public static final String ID = "philib";
    public static final Logger LOG = LogUtils.getLogger();

    public PhiLib(IEventBus bus) {
        LOG.info("Phi Lib: ready");
    }
}
