package by.deokma.stockmarket.fabric.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Guards all access to Create: Numismatics classes.
 * If the mod is absent, Vendor-related indexing is simply skipped.
 */
public final class NumismaticsCompat {

    private static final Logger LOGGER = LogManager.getLogger("stockmarket");
    private static final String MOD_ID = "numismatics";

    private static Boolean present = null;

    private NumismaticsCompat() {}

    /** Returns true if Create: Numismatics is loaded. Result is cached after first call. */
    public static boolean isPresent() {
        if (present == null) {
            present = FabricLoader.getInstance().isModLoaded(MOD_ID);
            LOGGER.info("[NumismaticsCompat] Create: Numismatics present = {}", present);
        }
        return present;
    }
}
