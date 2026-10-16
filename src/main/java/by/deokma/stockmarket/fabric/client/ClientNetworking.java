package by.deokma.stockmarket.fabric.client;

import by.deokma.stockmarket.fabric.network.ClientPacketHandler;
import by.deokma.stockmarket.fabric.network.MarketPacket;
import by.deokma.stockmarket.fabric.network.OpenShopListPacket;
import by.deokma.stockmarket.fabric.network.OpenStockMarketPacket;
import by.deokma.stockmarket.fabric.network.ShopListPacket;
import by.deokma.stockmarket.fabric.network.StockPacket;
import by.deokma.stockmarket.fabric.network.TradeStatsPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client packet receivers and client → server sending. Client only. */
@Environment(EnvType.CLIENT)
public final class ClientNetworking {

    private ClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ShopListPacket.ID,
                (client, handler, buf, responseSender) -> {
                    ShopListPacket pkt = ShopListPacket.read(buf);
                    client.execute(() -> ClientPacketHandler.handleShopList(pkt.entries()));
                });

        ClientPlayNetworking.registerGlobalReceiver(OpenShopListPacket.ID,
                (client, handler, buf, responseSender) -> {
                    OpenShopListPacket.read(buf);
                    client.execute(ClientPacketHandler::handleOpenShopList);
                });

        ClientPlayNetworking.registerGlobalReceiver(MarketPacket.ID,
                (client, handler, buf, responseSender) -> {
                    MarketPacket pkt = MarketPacket.read(buf);
                    client.execute(() -> ClientPacketHandler.handleMarketData(pkt.entries(), pkt.hotVolume()));
                });

        ClientPlayNetworking.registerGlobalReceiver(OpenStockMarketPacket.ID,
                (client, handler, buf, responseSender) -> {
                    OpenStockMarketPacket.read(buf);
                    client.execute(ClientPacketHandler::handleOpenStockMarket);
                });

        ClientPlayNetworking.registerGlobalReceiver(TradeStatsPacket.ID,
                (client, handler, buf, responseSender) -> {
                    TradeStatsPacket pkt = TradeStatsPacket.read(buf);
                    client.execute(() -> ClientPacketHandler.handleTradeStats(pkt.topSellers()));
                });
    }

    public static void send(StockPacket pkt) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        pkt.write(buf);
        ClientPlayNetworking.send(pkt.id(), buf);
    }
}
