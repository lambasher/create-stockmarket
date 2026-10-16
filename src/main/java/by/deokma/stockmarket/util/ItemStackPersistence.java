package by.deokma.stockmarket.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * Item NBT stores the stack {@code Count} as a signed byte on 1.20.1, so counts above 127 wrap
 * around (e.g. 192 logs would come back as -64). To stay well clear of that, stacks are
 * serialised with at most {@link #MAX_CODEC_COUNT} items and the real count is stored beside
 * them.
 */
public final class ItemStackPersistence {

    /** Largest count written through {@link ItemStack#save} — safely inside a signed byte. */
    public static final int MAX_CODEC_COUNT = 99;

    private static final String WRAP_ITEM = "CSM_Item";
    private static final String WRAP_COUNT = "CSM_FullCount";

    private ItemStackPersistence() {}

    /** Copy of {@code stack} with a different count ({@code ItemStack#copyWithCount} is 1.20.5+). */
    public static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    /** Writes non-empty stack under {@code key}; adds {@code key + "FullCount"} when count &gt; 99. */
    public static void writeIntoTag(CompoundTag parent, String key, ItemStack stack) {
        if (stack.isEmpty()) return;
        int count = stack.getCount();
        ItemStack forCodec = count > MAX_CODEC_COUNT ? withCount(stack, MAX_CODEC_COUNT) : stack;
        parent.put(key, forCodec.save(new CompoundTag()));
        if (count > MAX_CODEC_COUNT) {
            parent.putInt(key + "FullCount", count);
        }
    }

    public static ItemStack readFromTag(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.of(parent.getCompound(key));
        if (stack.isEmpty()) return stack;
        String overflow = key + "FullCount";
        if (parent.contains(overflow, Tag.TAG_INT)) {
            int full = parent.getInt(overflow);
            if (full > stack.getCount()) {
                stack = withCount(stack, full);
            }
        }
        return stack;
    }

    /** Network: one NBT payload — flat item tag or wrapper with full count. */
    public static void writeToNetworkBuf(FriendlyByteBuf buf, ItemStack stack) {
        if (stack.isEmpty()) {
            buf.writeNbt(null);
            return;
        }
        int count = stack.getCount();
        if (count <= MAX_CODEC_COUNT) {
            buf.writeNbt(stack.save(new CompoundTag()));
            return;
        }
        CompoundTag wrap = new CompoundTag();
        wrap.putInt(WRAP_COUNT, count);
        wrap.put(WRAP_ITEM, withCount(stack, MAX_CODEC_COUNT).save(new CompoundTag()));
        buf.writeNbt(wrap);
    }

    public static ItemStack readFromNetworkBuf(FriendlyByteBuf buf) {
        return parseNetworkNbt(buf.readNbt());
    }

    /** Parses payload from {@link #writeToNetworkBuf} (flat item tag or CSM wrapper). */
    public static ItemStack parseNetworkNbt(CompoundTag tag) {
        if (tag == null) return ItemStack.EMPTY;
        if (tag.contains(WRAP_ITEM, Tag.TAG_COMPOUND)) {
            int full = tag.getInt(WRAP_COUNT);
            ItemStack inner = ItemStack.of(tag.getCompound(WRAP_ITEM));
            if (inner.isEmpty() || full <= 0) return inner;
            return full > inner.getCount() ? withCount(inner, full) : inner;
        }
        return ItemStack.of(tag);
    }
}
