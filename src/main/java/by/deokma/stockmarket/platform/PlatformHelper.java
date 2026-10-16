package by.deokma.stockmarket.platform;

import java.util.function.BooleanSupplier;

/**
 * Loader-agnostic access to platform-specific feature checks.
 *
 * The Fabric entrypoint injects its checkers at startup, via
 * {@code FabricLoader.getInstance().isModLoaded(...)}.
 *
 * Defaults to {@code false} for all checks so common code is safe before injection.
 */
public final class PlatformHelper {

    private static BooleanSupplier numismaticsPresent = () -> false;

    private PlatformHelper() {}

    // ── Setters (called once at mod startup by the platform entrypoint) ────────

    public static void setNumismaticsChecker(BooleanSupplier supplier) {
        numismaticsPresent = supplier;
    }

    // ── Queries (used by common code) ─────────────────────────────────────────

    public static boolean isNumismaticsPresent() {
        return numismaticsPresent.getAsBoolean();
    }
}
