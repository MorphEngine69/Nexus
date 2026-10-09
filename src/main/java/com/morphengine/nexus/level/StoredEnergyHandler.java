package com.morphengine.nexus.level;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The energy in the cells of a network's storage as an {@link IEnergyStorage}. Changes apply at once.
 * Server thread only.
 */
final class StoredEnergyHandler implements IEnergyStorage {

    private final EnergyBuffer stored;

    /**
     * @param stored the storage's energy, shared with the network's pool
     */
    StoredEnergyHandler(final EnergyBuffer stored) {
        this.stored = stored;
    }

    @Override
    public int receiveEnergy(final int amount, final boolean simulate) {
        return amount <= 0 ? 0 : (int) stored.insert(amount, simulate ? Action.SIMULATE : Action.EXECUTE);
    }

    @Override
    public int extractEnergy(final int amount, final boolean simulate) {
        return amount <= 0 ? 0 : (int) stored.extract(amount, simulate ? Action.SIMULATE : Action.EXECUTE);
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(stored.stored(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(stored.capacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
