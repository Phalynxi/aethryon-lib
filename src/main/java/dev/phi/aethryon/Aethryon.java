package dev.phi.aethryon;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Aethryon: what Phalynxi's mods share. Mods depend on this and not on each other: each one registers what it knows (wind, clouds, wetness, disasters, ...)
 * with {@link dev.phi.aethryon.service.Services} and reads what the others know through {@link dev.phi.aethryon.api.AethryonApi}, which falls back to sensible vanilla-based defaults
 * when nobody provides something, so every mod also works alone.
 */
@Mod(Aethryon.ID)
public final class Aethryon {
    public static final String ID = "aethryon";
    public static final Logger LOG = LogUtils.getLogger();

    public Aethryon(IEventBus bus) {
        LOG.info("Aethryon: ready");
    }
}
