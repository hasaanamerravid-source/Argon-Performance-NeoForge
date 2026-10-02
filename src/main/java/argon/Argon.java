package argon;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Argon.MOD_ID)
public final class Argon {
    public static final String MOD_ID = "argon";
    public static final String VERSION = "0.2.4+26.3";
    public static final Logger LOGGER = LoggerFactory.getLogger("Argon");

    public Argon() {
        LOGGER.info("Argon {} layout optimizer ({})", VERSION, LoaderNames.current());
    }
}
