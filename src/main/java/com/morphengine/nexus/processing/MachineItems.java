package com.morphengine.nexus.processing;

import com.morphengine.nexus.machine.MachineInventory;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.transfer.CombinedItemHandler;
import com.morphengine.nexus.transfer.ItemResource;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What a machine shows pipes, hoppers and a Pusher through a side: its input slots where the side lets things in, its
 * output slots where it lets them out, both where it lets both. The input slots take only what has a recipe and share
 * it out as the input mode says.
 */
public final class MachineItems {

    private final IItemHandler intake;
    private final IItemHandler output;
    private final IItemHandler both;

    /**
     * @param accepts whether an item has a recipe, so that the input slots take it
     * @param changed called when a handler changed a slot
     */
    public MachineItems(
            final ItemStackSlots slots, final MachineInventory inventory, final Predicate<ItemResource> accepts,
            final Runnable changed) {
        this.intake = new StacksHandler(slots.inputStacks(), inventory, true, accepts, changed);
        this.output = new StacksHandler(slots.outputStacks(), inventory, false, accepts, changed);
        this.both = new CombinedItemHandler(intake, output);
    }

    /**
     * @return the handler for a side with {@code mode}, {@code null} when the side is closed
     */
    public @Nullable IItemHandler handlerFor(final SideMode mode) {
        return switch (mode) {
            case CLOSED -> null;
            case INPUT -> intake;
            case OUTPUT -> output;
            case BOTH -> both;
        };
    }

    /**
     * The stacks of one half of the slots, which only takes things in or only gives them out. The stacks are the
     * slots the machine works on.
     */
    private static final class StacksHandler extends ItemStackHandler {

        private final MachineInventory inventory;
        private final boolean takesIn;
        private final Predicate<ItemResource> accepts;
        private final Runnable changed;

        StacksHandler(
                final NonNullList<ItemStack> stacks, final MachineInventory inventory, final boolean takesIn,
                final Predicate<ItemResource> accepts, final Runnable changed) {
            super(stacks);
            this.inventory = inventory;
            this.takesIn = takesIn;
            this.accepts = accepts;
            this.changed = changed;
        }

        @Override
        public int getSlots() {
            return takesIn ? inventory.inputCount() : inventory.lineCount();
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return takesIn && slot < getSlots() && accepts.test(ItemResource.of(stack));
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (!takesIn || stack.isEmpty() || slot >= getSlots()) {
                return stack;
            }
            final ItemResource resource = ItemResource.of(stack);
            if (!accepts.test(resource)) {
                return stack;
            }
            final long[] parts = inventory.planInsert(new ItemKey(resource), stack.getCount());
            final int allowed = slot < parts.length ? (int) parts[slot] : 0;
            if (allowed <= 0) {
                return stack;
            }
            final ItemStack offered = stack.copyWithCount(Math.min(stack.getCount(), allowed));
            final ItemStack left = super.insertItem(slot, offered, simulate);
            final int inserted = offered.getCount() - left.getCount();
            return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return takesIn || slot >= getSlots() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            changed.run();
        }
    }
}
