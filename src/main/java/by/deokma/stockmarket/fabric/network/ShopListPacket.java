package by.deokma.stockmarket.fabric.network;

import by.deokma.stockmarket.shop.ShopEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Sent server → client with the full shop list. */
public record ShopListPacket(List<ShopEntry> entries) implements StockPacket {

    public static final ResourceLocation ID = StockPacket.channel("shop_list");

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (ShopEntry e : entries) e.write(buf);
    }

    public static ShopListPacket read(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ShopEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(ShopEntry.read(buf));
        return new ShopListPacket(list);
    }
}
