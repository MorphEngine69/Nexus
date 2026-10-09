package com.morphengine.nexus.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Objects;

/**
 * Two item handlers seen as one: the slots of the first, then the slots of the second. The sizes are asked for on every
 * call, as they change while the machine behind them is upgraded.
 */
public final class CombinedItemHandler implements IItemHandler {

    private final IItemHandler first;
    private final IItemHandler second;

    public CombinedItemHandler(final IItemHandler first, final IItemHandler second) {
        this.first = Objects.requireNonNull(first, "first must not be null");
        this.second = Objects.requireNonNull(second, "second must not be null");
    }

    @Override
    public int getSlots() {
        return first.getSlots() + second.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return slot < first.getSlots() ? first.getStackInSlot(slot) : second.getStackInSlot(slot - first.getSlots());
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        return slot < first.getSlots()
                ? first.insertItem(slot, stack, simulate)
                : second.insertItem(slot - first.getSlots(), stack, simulate);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return slot < first.getSlots()
                ? first.extractItem(slot, amount, simulate)
                : second.extractItem(slot - first.getSlots(), amount, simulate);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return slot < first.getSlots() ? first.getSlotLimit(slot) : second.getSlotLimit(slot - first.getSlots());
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return slot < first.getSlots()
                ? first.isItemValid(slot, stack)
                : second.isItemValid(slot - first.getSlots(), stack);
    }
}
