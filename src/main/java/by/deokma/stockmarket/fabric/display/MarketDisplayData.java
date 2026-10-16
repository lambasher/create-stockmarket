package by.deokma.stockmarket.fabric.display;

import by.deokma.stockmarket.market.MarketEntry;
import by.deokma.stockmarket.market.MarketRegistry;
import by.deokma.stockmarket.market.TradeStatsSavedData;
import by.deokma.stockmarket.shop.ShopEntry;
import by.deokma.stockmarket.shop.VendorRegistry;
import net.createmod.catnip.data.LongAttached;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Server-side data providers for the Market Terminal's Display Link sources.
 * Each value-list method returns an already-sorted stream; the source applies the row limit.
 */
public final class MarketDisplayData {

    private MarketDisplayData() {
    }

    // ── Value lists (leaderboards) ────────────────────────────────────────────

    /** Players ranked by completed sales. */
    public static Stream<LongAttached<MutableComponent>> topSellers(MinecraftServer server) {
        return TradeStatsSavedData.getOrCreate(server).getTopSellers(Integer.MAX_VALUE).stream()
                .map(e -> attach(e.getValue(), Component.literal(e.getKey())));
    }

    /** Players ranked by how many shop listings they own. */
    public static Stream<LongAttached<MutableComponent>> topShops(MinecraftServer server) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ShopEntry e : VendorRegistry.getAll()) counts.merge(e.ownerName(), 1L, Long::sum);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> attach(e.getValue(), Component.literal(e.getKey())));
    }

    /** Items ranked by total traded volume (sells + buys). */
    public static Stream<LongAttached<MutableComponent>> mostTraded(MinecraftServer server) {
        // Cached: every Display Link polls its source on its own schedule.
        return MarketRegistry.buildCached(server).stream()
                .filter(m -> m.sellCount() + m.buyCount() > 0)
                .sorted(Comparator.comparingInt((MarketEntry m) -> m.sellCount() + m.buyCount()).reversed())
                .map(m -> LongAttached.with(m.sellCount() + m.buyCount(), itemName(m)));
    }

    // ── Single-line counts ────────────────────────────────────────────────────

    /** Total number of shop listings (offers). */
    public static long shopCount(MinecraftServer server) {
        return VendorRegistry.getAll().size();
    }

    /** Number of distinct players running shops. */
    public static long sellerCount(MinecraftServer server) {
        return VendorRegistry.getAll().stream().map(ShopEntry::ownerName).distinct().count();
    }

    /** Sum of all completed sales across every seller. */
    public static long totalSales(MinecraftServer server) {
        return TradeStatsSavedData.getOrCreate(server).getTopSellers(Integer.MAX_VALUE).stream()
                .mapToLong(Map.Entry::getValue).sum();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static MutableComponent itemName(MarketEntry m) {
        return m.label().copy();
    }

    private static LongAttached<MutableComponent> attach(long value, MutableComponent label) {
        return LongAttached.with(value, label);
    }
}
