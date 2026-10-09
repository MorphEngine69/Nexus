package com.morphengine.nexus.item;

import com.morphengine.nexus.registry.NexusDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The charge of a Nexus Terminal as an energy storage of the game: chargers fill it, nothing takes energy out of it.
 */
final class TerminalChargeStorage implements IEnergyStorage {

    private final ItemStack stack;

    TerminalChargeStorage(final ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(final int amount, final boolean simulate) {
        final int stored = getEnergyStored();
        final int accepted = Math.max(0, Math.min(amount, getMaxEnergyStored() - stored));
        if (accepted > 0 && !simulate) {
            stack.set(NexusDataComponents.TERMINAL_CHARGE.get(), stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(final int amount, final boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return stack.getOrDefault(NexusDataComponents.TERMINAL_CHARGE.get(), 0);
    }

    @Override
    public int getMaxEnergyStored() {
        return NexusTerminalItem.capacity();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
