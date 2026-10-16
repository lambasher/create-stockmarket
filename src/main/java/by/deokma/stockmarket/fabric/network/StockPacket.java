package by.deokma.stockmarket.fabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * A custom-payload packet carried over Fabric API networking.
 * Each packet is a record that knows its channel id and how to write itself;
 * the matching static {@code read(FriendlyByteBuf)} lives on the record.
 */
public interface StockPacket {

    ResourceLocation id();

    void write(FriendlyByteBuf buf);

    static ResourceLocation channel(String path) {
        return new ResourceLocation("stockmarket", path);
    }
}
