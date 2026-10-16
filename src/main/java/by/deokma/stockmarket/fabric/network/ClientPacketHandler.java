package by.deokma.stockmarket.fabric.network;

import by.deokma.stockmarket.market.MarketData;
import by.deokma.stockmarket.market.MarketEntry;
import by.deokma.stockmarket.market.TradeStatsData;
import by.deokma.stockmarket.fabric.client.ShopListScreen;
import by.deokma.stockmarket.fabric.client.StockMarketScreen;
import by.deokma.stockmarket.shop.ShopEntry;
import by.deokma.stockmarket.shop.ShopListData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * All client-side packet handling logic lives here.
 * This class is only loaded on the CLIENT environment — never on the dedicated server.
 */
@Environment(EnvType.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void handleShopList(List<ShopEntry> entries) {
        ShopListData.set(entries);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof StockMarketScreen sms) {
            sms.refreshShopEntries();
        } else if (mc.screen instanceof ShopListScreen screen) {
            screen.refreshEntries();
        }
    }

    public static void handleOpenShopList() {
        Minecraft.getInstance().setScreen(new ShopListScreen());
    }

    public static void handleMarketData(List<MarketEntry> entries, int hotVolume) {
        MarketData.set(entries);
        MarketData.setHotVolume(hotVolume);
        MarketData.setLoading(false);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof StockMarketScreen screen) {
            screen.refreshEntries();
        }
    }

    public static void handleOpenStockMarket() {
        Minecraft.getInstance().setScreen(new StockMarketScreen());
    }

    public static void handleTradeStats(java.util.List<java.util.Map.Entry<String, Long>> topSellers) {
        TradeStatsData.set(topSellers);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof StockMarketScreen screen) {
            screen.refreshTopSellers();
        }
    }
}
