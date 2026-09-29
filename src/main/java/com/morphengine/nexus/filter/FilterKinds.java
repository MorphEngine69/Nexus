package com.morphengine.nexus.filter;

import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.ResourceTypes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

/**
 * Which kinds of resource a filter lists, and so what an item put on one of its
 * slots stands for: the item itself, or the fluid a container holds.
 */
public enum FilterKinds {

    /** Lists nothing: a filter that is fixed, such as that of a device moving energy. */
    NONE(false, false),
    ITEMS(true, false),
    FLUIDS(false, true);

    private final boolean listsItems;
    private final boolean listsFluids;

    FilterKinds(final boolean listsItems, final boolean listsFluids) {
        this.listsItems = listsItems;
        this.listsFluids = listsFluids;
    }

    /**
     * @return whether the player can list anything in such a filter; a filter
     *         that lists nothing is fixed and its slots are locked
     */
    public boolean listsAnything() {
        return listsItems || listsFluids;
    }

    public boolean accepts(final NexusResource resource) {
        return resource.type() == ResourceTypes.ITEM.get() ? listsItems
                : resource.type() == ResourceTypes.FLUID.get() && listsFluids;
    }

    /**
     * What a click with {@code stack} on a filter slot lists: the fluid in a
     * filled container where fluids are listed, otherwise the item.
     *
     * @return {@code null} when the stack lists nothing here
     */
    public @Nullable NexusResource contentsOf(final ItemStack stack) {
        final NexusResource fluid = listsFluids ? fluidOf(FluidUtil.getFirstStackContained(stack)) : null;
        return fluid != null ? fluid : itemOf(stack);
    }

    /**
     * What {@code stack} lists as itself: the item where items are listed; in a
     * filter of fluids only, the fluid it holds.
     *
     * @return {@code null} when the stack lists nothing here
     */
    public @Nullable NexusResource itemOf(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (listsItems) {
            return ItemKey.of(stack);
        }
        return fluidOf(FluidUtil.getFirstStackContained(stack));
    }

    /**
     * @return {@code fluid} as a filter entry; {@code null} for an empty stack
     *         or a filter without fluids
     */
    public @Nullable NexusResource fluidOf(final FluidStack fluid) {
        return listsFluids && !fluid.isEmpty() ? new FluidKey(FluidResource.of(fluid)) : null;
    }
}
