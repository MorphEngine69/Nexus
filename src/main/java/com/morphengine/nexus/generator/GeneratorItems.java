package com.morphengine.nexus.generator;

import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What a generator shows pipes, hoppers and a Pusher of its input slot through a side: where the side lets things in,
 * the fuel or the container of the fluid it burns; where it lets things out, only what the generator is done with, such
 * as an emptied bucket, never the fuel itself. Server thread only.
 */
public final class GeneratorItems {

    private final ResourceHandler<ItemResource> intake;
    private final ResourceHandler<ItemResource> output;
    private final ResourceHandler<ItemResource> both;

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
    public @Nullable ResourceHandler<ItemResource> handlerFor(final SideMode mode) {
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
     * The slot as a handler of the game. The superclass copies the list it is given, so the constructor puts the
     * original back: the stacks are the slot the generator works on.
     */
    private static final class Stacks extends ItemStacksResourceHandler {

        private final Rules rules;
        private final SideMode mode;
        private final Runnable changed;

        Stacks(final NonNullList<ItemStack> stacks, final Rules rules, final SideMode mode, final Runnable changed) {
            super(stacks);
            this.stacks = stacks;
            this.rules = rules;
            this.mode = mode;
            this.changed = changed;
        }

        @Override
        public boolean isValid(final int index, final ItemResource resource) {
            return mode.allowsInput() && rules.takesIn().test(resource) && super.isValid(index, resource);
        }

        @Override
        protected int getCapacity(final int index, final ItemResource resource) {
            return Math.min(rules.slotLimit(), super.getCapacity(index, resource));
        }

        @Override
        public int extract(
                final int index, final ItemResource resource, final int amount, final TransactionContext transaction) {
            return mode.allowsOutput() && rules.givesOut().test(resource)
                    ? super.extract(index, resource, amount, transaction) : 0;
        }

        @Override
        protected void onContentsChanged(final int index, final ItemStack previousContents) {
            changed.run();
        }
    }
}
