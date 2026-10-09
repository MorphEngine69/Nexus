package com.morphengine.nexus.generator;

import com.morphengine.nexus.transfer.ItemResource;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What a generator shows pipes, hoppers and a Pusher of its input slot through a side: where the side lets things in,
 * the fuel or the container of the fluid it burns; where it lets things out, only what the generator is done with, such
 * as an emptied bucket, never the fuel itself. Server thread only.
 */
public final class GeneratorItems {

    private final IItemHandler intake;
    private final IItemHandler output;
    private final IItemHandler both;

    /**
     * @param slot    the stacks of the input slot; the generator works on these very stacks
     * @param rules   what the slot takes in and gives out
     * @param changed called when a handler changed the slot
     */
    public GeneratorItems(final NonNullList<ItemStack> slot, final Rules rules, final Runnable changed) {
        this.intake = new Stacks(slot, rules, SideMode.INPUT, changed);
        this.output = new Stacks(slot, rules, SideMode.OUTPUT, changed);
        this.both = new Stacks(slot, rules, SideMode.BOTH, changed);
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
     * @param takesIn   whether the slot takes an item in
     * @param givesOut  whether an item may be taken out of the slot
     * @param slotLimit most items the slot holds, positive
     */
    public record Rules(Predicate<ItemResource> takesIn, Predicate<ItemResource> givesOut, int slotLimit) {
    }

    /**
     * The slot as a handler of the game. The stacks are the slot the generator works on.
     */
    private static final class Stacks extends ItemStackHandler {

        private final Rules rules;
        private final SideMode mode;
        private final Runnable changed;

        Stacks(final NonNullList<ItemStack> stacks, final Rules rules, final SideMode mode, final Runnable changed) {
            super(stacks);
            this.rules = rules;
            this.mode = mode;
            this.changed = changed;
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return mode.allowsInput() && rules.takesIn().test(ItemResource.of(stack));
        }

        @Override
        public int getSlotLimit(final int slot) {
            return Math.min(rules.slotLimit(), super.getSlotLimit(slot));
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return mode.allowsOutput() && rules.givesOut().test(ItemResource.of(getStackInSlot(slot)))
                    ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            changed.run();
        }
    }
}
