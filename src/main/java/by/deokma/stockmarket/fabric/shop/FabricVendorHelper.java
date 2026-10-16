package by.deokma.stockmarket.fabric.shop;

import by.deokma.stockmarket.fabric.compat.NumismaticsCompat;
import by.deokma.stockmarket.fabric.compat.VendorIndexer;
import by.deokma.stockmarket.platform.IPlatformVendorHelper;
import by.deokma.stockmarket.shop.ShopEntry;
import by.deokma.stockmarket.shop.ShopSavedData;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Fabric implementation of {@link IPlatformVendorHelper}.
 *
 * Handles the two shop types:
 * <ul>
 *   <li>Create TableCloth (always)</li>
 *   <li>Numismatics Vendor (when Numismatics is loaded)</li>
 * </ul>
 *
 * Chunk iteration goes through {@code ChunkMap.getChunks()}, made accessible by
 * {@code stockmarket.accesswidener}.
 */
public final class FabricVendorHelper implements IPlatformVendorHelper {

    private static final Logger LOGGER = LogManager.getLogger("stockmarket");

    // ── IPlatformVendorHelper ─────────────────────────────────────────────────

    @Override
    public boolean isShopEntity(BlockEntity be) {
        if (be instanceof TableClothBlockEntity) return true;
        return NumismaticsCompat.isPresent() && VendorIndexer.isVendorEntity(be);
    }

    @Override
    public boolean tryIndex(ServerLevel level, BlockEntity be,
                            ShopSavedData store, Map<UUID, String> nameCache) {
        // Numismatics Vendor — only if mod is present
        if (NumismaticsCompat.isPresent() && VendorIndexer.isVendorEntity(be)) {
            VendorIndexer.indexVendor(level, be, store, nameCache);
            return true;
        }
        // Create TableCloth
        if (be instanceof TableClothBlockEntity cloth) {
            indexTableCloth(level, cloth, store, nameCache);
            return true;
        }
        return false;
    }

    @Override
    public void forEachLoadedChunk(MinecraftServer server, BiConsumer<ServerLevel, LevelChunk> action) {
        for (ServerLevel level : server.getAllLevels()) {
            for (ChunkHolder holder : level.getChunkSource().chunkMap.getChunks()) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk == null) continue;
                action.accept(level, chunk);
            }
        }
    }

    // ── TableCloth indexing ───────────────────────────────────────────────────

    /**
     * Reads the cloth through Create's own fields rather than its NBT: the owner, the offered
     * stacks ({@code requestData.encodedRequest}) and the price ({@code getPaymentItem()} /
     * {@code getPaymentAmount()}) are all public on Create Fabric 1.20.1. That also avoids
     * serialising every cloth on every scan.
     */
    private static void indexTableCloth(ServerLevel level, TableClothBlockEntity cloth,
                                        ShopSavedData savedData, Map<UUID, String> nameCache) {
        try {
            MinecraftServer server = level.getServer();
            if (server == null) return;

            BlockPos pos = cloth.getBlockPos();

            // Cleared up front: a cloth that lost its owner or its offers is no longer a shop,
            // and returning early used to leave its old listings on the market forever.
            String baseKey = makeKey(level, pos);
            savedData.removeByBaseKey(baseKey);

            UUID ownerUuid = cloth.owner;
            if (ownerUuid == null) return;

            List<BigItemStack> offers = cloth.requestData.encodedRequest.stacks();
            if (offers.isEmpty()) return;

            ItemStack paymentTemplate = paymentItem(cloth);

            String ownerName = nameCache.computeIfAbsent(ownerUuid, id -> {
                var player = server.getPlayerList().getPlayer(id);
                if (player != null) return player.getName().getString();
                var cache = server.getProfileCache();
                if (cache != null) {
                    var opt = cache.get(id);
                    if (opt.isPresent() && opt.get().getName() != null) return opt.get().getName();
                }
                return id.toString().substring(0, 8);
            });

            int indexedRows = 0;
            for (int i = 0; i < offers.size(); i++) {
                BigItemStack offer = offers.get(i);
                if (offer == null || offer.stack == null || offer.stack.isEmpty()) continue;

                ItemStack sellingItem = offer.stack.copy();
                if (offer.count > 0) sellingItem.setCount(offer.count);

                ItemStack paymentItem = paymentTemplate.isEmpty()
                        ? ItemStack.EMPTY
                        : paymentTemplate.copy();

                savedData.put(baseKey + "#" + i, new ShopEntry(
                        pos,
                        level.dimension().location().toString(),
                        sellingItem,
                        0,
                        paymentItem,
                        ownerUuid,
                        ownerName,
                        "SELL",
                        "TABLECLOTH",
                        i
                ));
                indexedRows++;
            }
            if (indexedRows > 0) {
                LOGGER.debug("[FabricVendorHelper] TableCloth at {} indexed {} offer row(s)", pos, indexedRows);
            }
        } catch (Exception e) {
            LOGGER.debug("[FabricVendorHelper] indexTableCloth failed: {}", e.getMessage());
        }
    }

    /**
     * Payment item type comes from the price-tag filter; the required amount is stored separately
     * ({@code getPaymentAmount()}). The filter stack's own count is not the price — it often
     * mirrors a full slot (64) while the real price is 1.
     */
    private static ItemStack paymentItem(TableClothBlockEntity cloth) {
        ItemStack filter = cloth.getPaymentItem();
        if (filter == null || filter.isEmpty()) return ItemStack.EMPTY;

        ItemStack payment = filter.copy();
        // Create's table cloth price UI is 1–100; not limited by maxStackSize.
        payment.setCount(Mth.clamp(cloth.getPaymentAmount(), 1, 100));
        return payment;
    }

    private static String makeKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
