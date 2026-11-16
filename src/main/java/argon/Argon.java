package argon;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entry point.
 *
 * <p>The layout mixins run on whichever side generates chunks, so the mod itself is loaded on
 * both the client and the dedicated server. The frame cadence and the debug line are client
 * only and are kept in their own client mixins.
 */
@Mod(Argon.MOD_ID)
public final class Argon {
    public static final String MOD_ID = "argon";
    public static final String VERSION = "0.3.0+26.3";
    public static final Logger LOGGER = LoggerFactory.getLogger("Argon");

    public Argon() {
        LOGGER.info("Argon {} layout optimizer ({})", VERSION, LoaderNames.current());
    }
}
