package by.deokma.stockmarket.fabric;

import by.deokma.stockmarket.CreateStockMarket;
import by.deokma.stockmarket.block.MarketTerminalBlock;
import by.deokma.stockmarket.block.MarketTerminalBlockEntity;
import by.deokma.stockmarket.item.MarketRemoteItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Registers the mod's blocks, items and block entity types straight into the vanilla registries. */
public final class ModBlocks {

    public static MarketTerminalBlock MARKET_TERMINAL;
    public static BlockItem MARKET_TERMINAL_ITEM;

    /** Handheld remote — opens the Stock Market screen from anywhere. */
    public static MarketRemoteItem MARKET_REMOTE;

    public static BlockEntityType<MarketTerminalBlockEntity> MARKET_TERMINAL_BE;

    private ModBlocks() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CreateStockMarket.MOD_ID, path);
    }

    /** Call once from the main entrypoint. */
    public static void register() {
        MARKET_TERMINAL = Registry.register(BuiltInRegistries.BLOCK, id("market_terminal"),
                new MarketTerminalBlock(
                        BlockBehaviour.Properties.of()
                                .strength(3.5f)
                                .sound(SoundType.METAL)
                                .requiresCorrectToolForDrops()
                                .noOcclusion()));

        MARKET_TERMINAL_ITEM = Registry.register(BuiltInRegistries.ITEM, id("market_terminal"),
                new BlockItem(MARKET_TERMINAL, new Item.Properties()));

        MARKET_REMOTE = Registry.register(BuiltInRegistries.ITEM, id("market_remote"),
                new MarketRemoteItem(new Item.Properties().stacksTo(1)));

        MARKET_TERMINAL_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("market_terminal"),
                BlockEntityType.Builder.of(MarketTerminalBlockEntity::new, MARKET_TERMINAL).build(null));

        // MarketTerminalBlockEntity looks its type up lazily through this supplier.
        MarketTerminalBlockEntity.setTypeSupplier(() -> MARKET_TERMINAL_BE);
    }
}
