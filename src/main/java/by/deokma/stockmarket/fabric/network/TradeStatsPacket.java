package by.deokma.stockmarket.fabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Sent server → client with the top-sellers leaderboard. */
public record TradeStatsPacket(List<Map.Entry<String, Long>> topSellers) implements StockPacket {

    public static final ResourceLocation ID = StockPacket.channel("trade_stats");

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(topSellers.size());
        for (var e : topSellers) {
            buf.writeUtf(e.getKey());
            buf.writeLong(e.getValue());
        }
    }

    public static TradeStatsPacket read(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<Map.Entry<String, Long>> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf();
            long count = buf.readLong();
            list.add(Map.entry(name, count));
        }
        return new TradeStatsPacket(list);
    }
}
