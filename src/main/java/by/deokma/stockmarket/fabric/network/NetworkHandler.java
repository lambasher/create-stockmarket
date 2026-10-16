package by.deokma.stockmarket.fabric.network;

import by.deokma.stockmarket.fabric.client.ClientNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet registration and sending, on top of Fabric API networking.
 *
 * <p>Client → server receivers are registered here (common entrypoint); the server → client
 * receivers live in {@link ClientNetworking} so no client class is touched on a dedicated server.
 */
public final class NetworkHandler {

    private NetworkHandler() {}

    /** Registers every client → server packet receiver. Call once from the main entrypoint. */
    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RequestShopListPacket.ID,
                (server, player, handler, buf, responseSender) -> {
                    RequestShopListPacket.read(buf);
                    server.execute(() -> RequestShopListPacket.handle(player));
                });

        ServerPlayNetworking.registerGlobalReceiver(RequestMarketPacket.ID,
                (server, player, handler, buf, responseSender) -> {
                    RequestMarketPacket.read(buf);
                    server.execute(() -> RequestMarketPacket.handle(player));
                });

        ServerPlayNetworking.registerGlobalReceiver(RequestTradeStatsPacket.ID,
                (server, player, handler, buf, responseSender) -> {
                    RequestTradeStatsPacket.read(buf);
                    server.execute(() -> RequestTradeStatsPacket.handle(player));
                });
    }

    /** Server → client. */
    public static void sendToPlayer(ServerPlayer player, StockPacket pkt) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        pkt.write(buf);
        ServerPlayNetworking.send(player, pkt.id(), buf);
    }

    /** Client → server. Only ever invoked from client code. */
    public static void sendToServer(StockPacket pkt) {
        ClientNetworking.send(pkt);
    }
}
