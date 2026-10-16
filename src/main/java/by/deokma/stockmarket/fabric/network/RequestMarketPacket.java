package by.deokma.stockmarket.fabric.network;

import by.deokma.stockmarket.config.MarketConfig;
import by.deokma.stockmarket.market.MarketEntry;
import by.deokma.stockmarket.market.MarketRegistry;
import by.deokma.stockmarket.market.TradeStatsSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Sent client → server to request market data. */
public record RequestMarketPacket() implements StockPacket {

    public static final ResourceLocation ID = StockPacket.channel("request_market");

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public void write(FriendlyByteBuf buf) {}

    public static RequestMarketPacket read(FriendlyByteBuf buf) { return new RequestMarketPacket(); }

    /** Runs on the server thread. */
    public static void handle(ServerPlayer player) {
        List<MarketEntry> entries = MarketRegistry.build(player.server);
        NetworkHandler.sendToPlayer(player, new MarketPacket(entries, MarketConfig.hotVolume()));
        // Also send trade stats leaderboard
        var stats = TradeStatsSavedData.getOrCreate(player.server);
        NetworkHandler.sendToPlayer(player, new TradeStatsPacket(stats.getTopSellers(10)));
    }
}
