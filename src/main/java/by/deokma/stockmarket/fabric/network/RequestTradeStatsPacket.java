package by.deokma.stockmarket.fabric.network;

import by.deokma.stockmarket.market.TradeStatsSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Sent client → server to request the full trade-stats leaderboard. */
public record RequestTradeStatsPacket() implements StockPacket {

    public static final ResourceLocation ID = StockPacket.channel("request_trade_stats");

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public void write(FriendlyByteBuf buf) {}

    public static RequestTradeStatsPacket read(FriendlyByteBuf buf) { return new RequestTradeStatsPacket(); }

    /** Runs on the server thread. */
    public static void handle(ServerPlayer player) {
        var stats = TradeStatsSavedData.getOrCreate(player.server);
        // Send all entries — no limit
        NetworkHandler.sendToPlayer(player, new TradeStatsPacket(stats.getTopSellers(Integer.MAX_VALUE)));
    }
}
