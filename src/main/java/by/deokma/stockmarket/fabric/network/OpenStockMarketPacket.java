package by.deokma.stockmarket.fabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Sent server → client to open the StockMarketScreen. */
public record OpenStockMarketPacket() implements StockPacket {

    public static final ResourceLocation ID = StockPacket.channel("open_stock_market");

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public void write(FriendlyByteBuf buf) {}

    public static OpenStockMarketPacket read(FriendlyByteBuf buf) { return new OpenStockMarketPacket(); }
}
