package by.deokma.stockmarket.fabric;

import by.deokma.stockmarket.CommonInit;
import by.deokma.stockmarket.CreateStockMarket;
import by.deokma.stockmarket.block.MarketTerminalBlock;
import by.deokma.stockmarket.command.ShopListCommand;
import by.deokma.stockmarket.item.MarketRemoteItem;
import by.deokma.stockmarket.fabric.compat.NumismaticsCompat;
import by.deokma.stockmarket.fabric.display.ModDisplaySources;
import by.deokma.stockmarket.fabric.market.MarketEvents;
import by.deokma.stockmarket.fabric.network.NetworkHandler;
import by.deokma.stockmarket.fabric.network.OpenShopListPacket;
import by.deokma.stockmarket.fabric.network.OpenStockMarketPacket;
import by.deokma.stockmarket.fabric.shop.FabricVendorHelper;
import by.deokma.stockmarket.fabric.shop.VendorEvents;
import by.deokma.stockmarket.platform.PlatformHelper;
import by.deokma.stockmarket.shop.VendorRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CreateStockMarketFabric implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger(CreateStockMarket.MOD_ID);

    /** Create's main creative tab ({@code create:base}) — where our block is listed so JEI/EMI/REI pick it up. */
    private static final ResourceKey<CreativeModeTab> CREATE_BASE_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, new ResourceLocation("create", "base"));

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Fabric entrypoint initialising", CreateStockMarket.MOD_ID);

        CommonInit.init();

        // ── Config ────────────────────────────────────────────────────────────
        // Registered first so MarketConfig is wired before anything reads it.
        StockMarketConfig.register();

        // ── Platform service injection ─────────────────────────────────────────
        // Inject platform-specific vendor helper (chunk iteration + shop indexing)
        VendorRegistry.setPlatformHelper(new FabricVendorHelper());

        // Inject mod-presence checker so common code can query without Numismatics imports
        PlatformHelper.setNumismaticsChecker(NumismaticsCompat::isPresent);

        // Inject network action for the /shoplist command
        ShopListCommand.setOpenShopListSender(player ->
                NetworkHandler.sendToPlayer(player, new OpenShopListPacket()));

        // ── Registration ──────────────────────────────────────────────────────
        ModBlocks.register();
        ModDisplaySources.register();

        // Hook: when a player right-clicks the Market Terminal, send OpenStockMarketPacket
        MarketTerminalBlock.setOpenScreenHandler(player ->
                NetworkHandler.sendToPlayer(player, new OpenStockMarketPacket()));

        // Same hook for the handheld Market Remote item (remote access from anywhere)
        MarketRemoteItem.setOpenScreenHandler(player ->
                NetworkHandler.sendToPlayer(player, new OpenStockMarketPacket()));

        // ── Network + events ──────────────────────────────────────────────────
        NetworkHandler.registerServerReceivers();
        VendorEvents.register();
        MarketEvents.register();

        // Adds the Market Terminal and Remote to Create's creative tab (inventory + JEI/EMI/REI).
        ItemGroupEvents.modifyEntriesEvent(CREATE_BASE_TAB).register(entries -> {
            entries.accept(ModBlocks.MARKET_TERMINAL_ITEM);
            entries.accept(ModBlocks.MARKET_REMOTE);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                ShopListCommand.register(dispatcher));

        LOGGER.info("[{}] Fabric init complete", CreateStockMarket.MOD_ID);
    }
}
