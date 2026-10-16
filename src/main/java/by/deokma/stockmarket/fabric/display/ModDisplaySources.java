package by.deokma.stockmarket.fabric.display;

import by.deokma.stockmarket.CreateStockMarket;
import by.deokma.stockmarket.fabric.ModBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Registers Create Display Link sources for the Market Terminal and binds them to the block,
 * so a Display Link can read live market data onto signs/boards/nixie tubes.
 *
 * <p>Two generic source types ({@link MarketValueListSource}, {@link MarketStatSource}) are
 * instantiated once per criterion — each registration name becomes the source id and its
 * translation key ({@code stockmarket.display_source.<name>}).
 *
 * <p>Call {@link #register()} after {@link ModBlocks#register()}.
 */
public final class ModDisplaySources {

    private ModDisplaySources() {
    }

    public static void register() {
        Block terminal = ModBlocks.MARKET_TERMINAL;

        // ── Leaderboards (multi-row) ──────────────────────────────────────────
        add(terminal, "top_sellers", new MarketValueListSource(MarketDisplayData::topSellers));
        add(terminal, "top_shops", new MarketValueListSource(MarketDisplayData::topShops));
        add(terminal, "most_traded", new MarketValueListSource(MarketDisplayData::mostTraded));

        // ── Scalar stats (single line) ────────────────────────────────────────
        add(terminal, "shop_count", new MarketStatSource(MarketDisplayData::shopCount));
        add(terminal, "seller_count", new MarketStatSource(MarketDisplayData::sellerCount));
        add(terminal, "total_sales", new MarketStatSource(MarketDisplayData::totalSales));
    }

    /** Registers the source under {@code stockmarket:<name>} and associates it with the block. */
    private static void add(Block block, String name, DisplaySource source) {
        Registry.register(CreateBuiltInRegistries.DISPLAY_SOURCE,
                new ResourceLocation(CreateStockMarket.MOD_ID, name), source);
        DisplaySource.BY_BLOCK.add(block, source);
    }
}
