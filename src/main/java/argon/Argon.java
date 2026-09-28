package argon;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Argon implements ModInitializer {
    public static final String VERSION = "0.2.3+26.3";
    public static final Logger LOGGER = LoggerFactory.getLogger("Argon-Optimizer-Fabric");

    @Override
    public void onInitialize() {
        LOGGER.info("Argon {} layout optimizer", VERSION);
    }
}
