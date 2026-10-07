package com.morphengine.nexus.processing;

import com.morphengine.nexus.machine.MachineInventory;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What a machine shows pipes, hoppers and a Pusher through a side: its input slots where the side lets things in, its
 * output slots where it lets them out, both where it lets both. The input slots take only what has a recipe and share
 * it out as the input mode says.
 */
public final class MachineItems {

    private final ResourceHandler<ItemResource> intake;
    private final ResourceHandler<ItemResource> output;
    private final ResourceHandler<ItemResource> both;

    /**
     * @param accepts whether an item has a recipe, so that the input slots take it
     * @param changed called when a handler changed a slot
     */
    public MachineItems(
            final ItemStackSlots slots, final MachineInventory inventory, final Predicate<ItemResource> accepts,
            final Runnable changed) {
        this.intake = new StacksHandler(slots.inputStacks(), inventory, true, accepts, changed);
        this.output = new StacksHandler(slots.outputStacks(), inventory, false, accepts, changed);
        this.both = new CombinedResourceHandler<>(intake, output);
    }

    /**
     * @return the handler for a side with {@code mode}, {@code null} when the side is closed
     */
    public @Nullable ResourceHandler<ItemResource> handlerFor(final SideMode mode) {
        return switch (mode) {
            case CLOSED -> null;
            case INPUT -> intake;
            case OUTPUT -> output;
            case BOTH -> both;
        };
    }

    /**
     * The stacks of one half of the slots, which only takes things in or only gives them out. The superclass copies the
     * list it is given, so the constructor puts the original back: the stacks are the slots the machine works on.
     */
    private static final class StacksHandler extends ItemStacksResourceHandler {

        private final MachineInventory inventory;
        private final boolean takesIn;
        private final Predicate<ItemResource> accepts;
        private final Runnable changed;

        StacksHandler(
                final NonNullList<ItemStack> stacks, final MachineInventory inventory, final boolean takesIn,
                final Predicate<ItemResource> accepts, final Runnable changed) {
            super(stacks);
            this.stacks = stacks;
            this.inventory = inventory;
            this.takesIn = takesIn;
            this.accepts = accepts;
            this.changed = changed;
        }

        @Override
        public int size() {
            return takesIn ? inventory.inputCount() : inventory.lineCount();
        }

        @Override
        public boolean isValid(final int index, final ItemResource resource) {
            return takesIn && index < size() && accepts.test(resource) && super.isValid(index, resource);
        }

        @Override
        public int insert(final ItemResource resource, final int amount, final TransactionContext transaction) {
            if (!takesIn || !accepts.test(resource)) {
                return 0;
            }
            final long[] parts = inventory.planInsert(new ItemKey(resource), amount);
            int accepted = 0;
            for (int index = 0; index < parts.length; index++) {
                if (parts[index] > 0) {
                    accepted += insert(index, resource, (int) parts[index], transaction);
                }
            }
            return accepted;
        }

        @Override
        public int extract(
                final int index, final ItemResource resource, final int amount, final TransactionContext transaction) {
            return takesIn || index >= size() ? 0 : super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(final int index, final ItemStack previousContents) {
            changed.run();
        }
    }
}
