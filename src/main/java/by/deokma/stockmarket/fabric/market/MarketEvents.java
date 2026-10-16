package by.deokma.stockmarket.fabric.market;

import by.deokma.stockmarket.config.MarketConfig;
import by.deokma.stockmarket.market.MarketRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class MarketEvents {

    private static int tickCounter = 0;

    private MarketEvents() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Interval is read every tick so a config reload takes effect without a restart.
            if (++tickCounter >= MarketConfig.snapshotIntervalTicks()) {
                tickCounter = 0;
                MarketRegistry.snapshot(server);
            }
        });
    }
}
