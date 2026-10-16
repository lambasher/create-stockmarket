package by.deokma.stockmarket.fabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Sent server → client to open the ShopListScreen. */
public record OpenShopListPacket() implements StockPacket {

    public static final ResourceLocation ID = StockPacket.channel("open_shop_list");

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public void write(FriendlyByteBuf buf) {}

    public static OpenShopListPacket read(FriendlyByteBuf buf) { return new OpenShopListPacket(); }
}
