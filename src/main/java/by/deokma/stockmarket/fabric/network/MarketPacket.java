package by.deokma.stockmarket.fabric.network;

import by.deokma.stockmarket.market.MarketEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Sent server → client with the aggregated market data. */
public record MarketPacket(List<MarketEntry> entries, int hotVolume) implements StockPacket {

    public static final ResourceLocation ID = StockPacket.channel("market_data");

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (MarketEntry e : entries) e.write(buf);
        buf.writeInt(hotVolume);
    }

    public static MarketPacket read(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<MarketEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(MarketEntry.read(buf));
        return new MarketPacket(list, buf.readInt());
    }
}
