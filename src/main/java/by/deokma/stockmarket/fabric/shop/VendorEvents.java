package by.deokma.stockmarket.fabric.shop;

import by.deokma.stockmarket.fabric.compat.NumismaticsCompat;
import by.deokma.stockmarket.fabric.compat.StockKeeperSaleTracker;
import by.deokma.stockmarket.fabric.compat.VendorIndexer;
import by.deokma.stockmarket.fabric.compat.VendorTransactionTracker;
import by.deokma.stockmarket.shop.VendorRegistry;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/**
 * Keeps the shop index in sync with the world.
 *
 * <p>TableCloth sales are not polled here: a cloth purchase is paid at the Stock Keeper,
 * which {@link StockKeeperSaleTracker} records. The per-second cloth poll that used to live
 * here serialised every cloth's NBT each second and could only ever see the owner editing
 * an offer, which it then counted as a sale.
 */
public final class VendorEvents {

    private static final int REFRESH_INTERVAL = 6000; // 5 minutes at 20 TPS

    private static int tickCounter = 0;

    private VendorEvents() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            tickCounter = 0;
            VendorRegistry.onServerStart(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            VendorRegistry.onServerStop();
            if (NumismaticsCompat.isPresent()) {
                VendorTransactionTracker.clear();
            }
            StockKeeperSaleTracker.clear();
        });

        // One hook covers both a chunk's block entities loading and a shop being placed.
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) ->
                VendorRegistry.onBlockEntityLoad(level, blockEntity));

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel && isShopBlock(state.getBlock())) {
                VendorRegistry.onBlockBreak(serverLevel, pos);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < REFRESH_INTERVAL) return;
            tickCounter = 0;
            VendorRegistry.refreshLoaded(server);
        });

        if (NumismaticsCompat.isPresent()) {
            VendorTransactionTracker.register();
        }
        StockKeeperSaleTracker.register();
    }

    private static boolean isShopBlock(Block block) {
        return block instanceof TableClothBlock
                || (NumismaticsCompat.isPresent() && VendorIndexer.isVendorBlock(block));
    }
}
