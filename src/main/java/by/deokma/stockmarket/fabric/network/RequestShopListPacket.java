package by.deokma.stockmarket.fabric.network;

import by.deokma.stockmarket.shop.ShopEntry;
import by.deokma.stockmarket.shop.VendorRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Sent client → server to request the shop list. */
public record RequestShopListPacket() implements StockPacket {

    public static final ResourceLocation ID = StockPacket.channel("request_shop_list");

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public void write(FriendlyByteBuf buf) {}

    public static RequestShopListPacket read(FriendlyByteBuf buf) { return new RequestShopListPacket(); }

    /** Runs on the server thread. */
    public static void handle(ServerPlayer player) {
        // Refresh loaded chunks to pick up any recent changes, then send all known shops.
        // Throttled: a full rescan per request let anyone lag the server by reopening the list.
        VendorRegistry.refreshLoadedThrottled(player.server);
        List<ShopEntry> entries = VendorRegistry.getAll();
        NetworkHandler.sendToPlayer(player, new ShopListPacket(entries));
    }
}
