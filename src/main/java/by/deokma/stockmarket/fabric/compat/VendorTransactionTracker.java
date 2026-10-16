package by.deokma.stockmarket.fabric.compat;

import by.deokma.stockmarket.market.TradeStatsSavedData;
import dev.ithundxr.createnumismatics.content.vendor.VendorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks Numismatics Vendor purchases by capturing stock before the interaction
 * and comparing on the next server tick — mirrors StockKeeperSaleTracker's approach.
 *
 * This class must ONLY be loaded when {@link NumismaticsCompat#isPresent()} is true.
 */
public final class VendorTransactionTracker {

    private static final Logger LOGGER = LogManager.getLogger("stockmarket");

    private record VendorSnapshot(BlockPos pos, String dimKey, int initialCount, UUID ownerUuid) {}

    /** Player UUID → state captured just before the vendor interaction. */
    private static final Map<UUID, VendorSnapshot> pendingChecks = new ConcurrentHashMap<>();

    private VendorTransactionTracker() {}

    public static void register() {
        // Early phase: runs before the default phase, i.e. before the vendor handles the click.
        ResourceLocation early = new ResourceLocation("stockmarket", "vendor_tracker_early");
        UseBlockCallback.EVENT.addPhaseOrdering(early, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.register(early, VendorTransactionTracker::onInteractEarly);
        ServerTickEvents.END_SERVER_TICK.register(VendorTransactionTracker::onServerTick);
    }

    /** Clears state on server stop. */
    public static void clear() {
        pendingChecks.clear();
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Fires before the vendor processes the purchase.
     * Captures the current stock count so we can compare after the tick.
     */
    private static InteractionResult onInteractEarly(Player p, Level world, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;
        if (!(p instanceof ServerPlayer player)) return InteractionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof VendorBlockEntity vendor)) return InteractionResult.PASS;

        try {
            if (vendor.getMode() != VendorBlockEntity.Mode.SELL) return InteractionResult.PASS;

            int stock = readStockCount(vendor, level.getServer());
            if (stock < 0) return InteractionResult.PASS;

            UUID ownerUuid = NumismaticsVendorAccess.owner(vendor);
            if (ownerUuid == null) return InteractionResult.PASS;

            String dimKey = level.dimension().location() + "|" + pos.toShortString();
            pendingChecks.put(player.getUUID(), new VendorSnapshot(pos, dimKey, stock, ownerUuid));
        } catch (Exception | LinkageError e) {
            // Never let a Numismatics API change break right-clicking a vendor.
            VendorIndexer.logFailure(e);
        }
        return InteractionResult.PASS;
    }

    /**
     * Runs at the end of the tick after the interaction — by then BlockBehaviour.use() has run
     * and the vendor inventory has been updated.
     */
    private static void onServerTick(MinecraftServer server) {
        if (pendingChecks.isEmpty()) return;

        var iterator = pendingChecks.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            iterator.remove();

            VendorSnapshot snapshot = entry.getValue();

            ServerLevel level = findLevel(server, snapshot.dimKey());
            if (level == null) continue;

            BlockEntity be = level.getBlockEntity(snapshot.pos());
            if (!(be instanceof VendorBlockEntity vendor)) continue;

            int newStock = readStockCount(vendor, server);
            if (newStock < 0 || newStock >= snapshot.initialCount()) continue;

            int sold = snapshot.initialCount() - newStock;
            String ownerName = resolveOwnerName(server, snapshot.ownerUuid());
            // Record 1 transaction per purchase, regardless of how many items
            // were in the stack — buying 64 blocks at once is still 1 sale.
            TradeStatsSavedData.getOrCreate(server).recordSale(ownerName, 1);

            LOGGER.debug("[VendorTracker] Sale at {} owner={} items={}",
                    snapshot.pos(), ownerName, sold);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Counts all items in the vendor's container that the vendor trades.
     * Uses Container interface directly — avoids fragile NBT key guessing.
     */
    private static int readStockCount(VendorBlockEntity vendor, MinecraftServer server) {
        try {
            ItemStack template = NumismaticsVendorAccess.tradedItem(vendor);
            if (template.isEmpty()) return -1;

            int total = 0;
            int size = vendor.getContainerSize();
            for (int i = 0; i < size; i++) {
                ItemStack stack = vendor.getItem(i);
                if (NumismaticsVendorAccess.matches(vendor, template, stack)) {
                    total += stack.getCount();
                }
            }
            return total;
        } catch (Exception | LinkageError e) {
            VendorIndexer.logFailure(e);
            return -1;
        }
    }

    private static ServerLevel findLevel(MinecraftServer server, String dimKey) {
        // The key is "<dimension>|<pos>"; a dimension id never contains '|'.
        ResourceLocation dimId = ResourceLocation.tryParse(dimKey.substring(0, dimKey.indexOf('|')));
        return dimId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
    }

    private static String resolveOwnerName(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getName().getString();
        var cache = server.getProfileCache();
        if (cache != null) {
            var opt = cache.get(uuid);
            if (opt.isPresent() && opt.get().getName() != null) return opt.get().getName();
        }
        return uuid.toString().substring(0, 8);
    }
}
