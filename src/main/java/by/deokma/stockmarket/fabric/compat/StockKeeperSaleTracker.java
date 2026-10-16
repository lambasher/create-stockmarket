package by.deokma.stockmarket.fabric.compat;

import by.deokma.stockmarket.CreateStockMarket;
import by.deokma.stockmarket.market.TradeStatsSavedData;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlock;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Counts Create Stock Keeper sales.
 *
 * <p>Works in two steps, because Fabric API has no "after the handler, even if cancelled" event:
 * <ol>
 *   <li>An early-phase interaction hook runs <em>before</em> Create's own handlers and remembers
 *       which shop owner the player's Shopping List belongs to. It never consumes the click.</li>
 *   <li>At the end of the same server tick, by which point Create has finished the checkout, a
 *       sale is recorded if the Shopping List is no longer in the player's hand.</li>
 * </ol>
 *
 * <p>Covers both seated mob vendors (entity interaction) and block-based vendors such as the
 * Blaze Burner or a Stock Ticker clicked directly (block interaction).
 */
public final class StockKeeperSaleTracker {

    private static final Logger LOGGER = LogManager.getLogger("stockmarket");

    /** Phase that runs before the default phase, where Create registers its own handlers. */
    private static final ResourceLocation EARLY_PHASE =
            new ResourceLocation(CreateStockMarket.MOD_ID, "stock_keeper_early");

    /** Player UUID -> shop owner UUID saved before checkout. */
    private static final Map<UUID, UUID> pendingShopOwner = new HashMap<>();

    private StockKeeperSaleTracker() {
    }

    public static void register() {
        UseEntityCallback.EVENT.addPhaseOrdering(EARLY_PHASE, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.addPhaseOrdering(EARLY_PHASE, Event.DEFAULT_PHASE);

        UseEntityCallback.EVENT.register(EARLY_PHASE, StockKeeperSaleTracker::onEntityInteract);
        UseBlockCallback.EVENT.register(EARLY_PHASE, StockKeeperSaleTracker::onBlockInteract);
        ServerTickEvents.END_SERVER_TICK.register(StockKeeperSaleTracker::onServerTick);
    }

    // ------------------------------------------------------------------------
    // PRE EVENTS (before Create handles the click)
    // ------------------------------------------------------------------------

    private static InteractionResult onEntityInteract(Player player, Level level, InteractionHand hand,
                                                      Entity entity, EntityHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        // Standard Create mob vendor detection
        if (StockTickerInteractionHandler.getStockTickerPosition(entity) == null) return InteractionResult.PASS;

        captureShoppingList(serverPlayer);
        return InteractionResult.PASS;
    }

    private static InteractionResult onBlockInteract(Player player, Level level, InteractionHand hand,
                                                     BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        var be = level.getBlockEntity(pos);
        if (!(be instanceof StockTickerBlockEntity ticker && ticker.isKeeperPresent())) {
            // Blaze Burner (or another block-based vendor) — look for a neighbouring Stock Ticker.
            BlockPos tickerPos = findAdjacentStockTicker(level, pos);
            if (tickerPos == null) return InteractionResult.PASS;
            if (!(level.getBlockEntity(tickerPos) instanceof StockTickerBlockEntity neighbour)) return InteractionResult.PASS;
            if (!neighbour.isKeeperPresent()) return InteractionResult.PASS;
        }

        captureShoppingList(serverPlayer);
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------------
    // CORE LOGIC
    // ------------------------------------------------------------------------

    /**
     * Saves shop owner UUID from the shopping list before Create processes the checkout.
     */
    private static void captureShoppingList(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (!AllItems.SHOPPING_LIST.isIn(main)) return;

        var list = ShoppingListItem.getList(main);
        if (list == null) return;

        pendingShopOwner.put(player.getUUID(), list.shopOwner());
    }

    /**
     * End of tick: Create has finished the interaction. If the Shopping List is gone, the
     * checkout completed and the sale is recorded.
     */
    private static void onServerTick(MinecraftServer server) {
        if (pendingShopOwner.isEmpty()) return;

        var iterator = pendingShopOwner.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            iterator.remove();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;

            UUID shopOwner = entry.getValue();

            // If the same shopping list is still present, checkout did not complete.
            ItemStack main = player.getMainHandItem();
            if (AllItems.SHOPPING_LIST.isIn(main)) {
                var list = ShoppingListItem.getList(main);
                if (list != null && shopOwner.equals(list.shopOwner())) continue;
            }

            String ownerName = resolveOwnerName(server, shopOwner);

            LOGGER.debug(
                    "[StockKeeperTracker] Stock Keeper sale recorded for owner {} (player {})",
                    ownerName,
                    player.getName().getString()
            );

            TradeStatsSavedData.getOrCreate(server).recordSale(ownerName, 1);
        }
    }

    // ------------------------------------------------------------------------
    // STOCK TICKER SEARCH
    // ------------------------------------------------------------------------

    /**
     * Finds a StockTicker adjacent to the clicked block.
     * Checks all 6 directions.
     */
    private static BlockPos findAdjacentStockTicker(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);

            if (!(level.getBlockState(neighbor).getBlock() instanceof StockTickerBlock))
                continue;

            if (level.getBlockEntity(neighbor) instanceof StockTickerBlockEntity)
                return neighbor;
        }

        return null;
    }

    // ------------------------------------------------------------------------
    // UTILITIES
    // ------------------------------------------------------------------------

    public static void clear() {
        pendingShopOwner.clear();
    }

    private static String resolveOwnerName(MinecraftServer server, UUID uuid) {
        var online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getName().getString();
        }

        var cache = server.getProfileCache();
        if (cache != null) {
            var opt = cache.get(uuid);
            if (opt.isPresent() && opt.get().getName() != null) {
                return opt.get().getName();
            }
        }

        return uuid.toString().substring(0, 8);
    }
}
