package argon;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("argon")
public final class ArgonNeoForge {
    public static final String VERSION = "0.2.2";
    public static final Logger LOGGER = LoggerFactory.getLogger("Argon");

    public ArgonNeoForge() {
        LOGGER.info("Argon {} layout optimizer", VERSION);
    }
}
