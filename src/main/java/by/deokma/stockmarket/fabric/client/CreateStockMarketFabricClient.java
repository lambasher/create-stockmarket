package by.deokma.stockmarket.fabric.client;

import by.deokma.stockmarket.fabric.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

@Environment(EnvType.CLIENT)
public final class CreateStockMarketFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientNetworking.register();

        BlockEntityRenderers.register(ModBlocks.MARKET_TERMINAL_BE, MarketTerminalRenderer::new);

        TooltipComponentCallback.EVENT.register(data ->
                data instanceof CoinPriceTooltip tooltip ? new CoinPriceTooltip.Renderer(tooltip) : null);
    }
}
