package com.morphengine.nexus.level;

import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/**
 * A network's energy as other mods reach it through its Nexus: inserts fill and
 * extracts drain the pool's buffers in the order the pool ranks them. The network's current buffers are read
 * on every call, so the handler stays valid across rebuilds. Server thread only.
 */
final class NetworkEnergyHandler implements IEnergyStorage {

    private final NetworkState network;

    NetworkEnergyHandler(final NetworkState network) {
        this.network = network;
    }

    @Override
    public int receiveEnergy(final int amount, final boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        final List<IEnergyStorage> handlers = network.energyHandlers().fillOrder();
        int remaining = amount;
        for (int i = 0; i < handlers.size() && remaining > 0; i++) {
            remaining -= handlers.get(i).receiveEnergy(remaining, simulate);
        }
        return amount - remaining;
    }

    @Override
    public int extractEnergy(final int amount, final boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        final List<IEnergyStorage> handlers = network.energyHandlers().drainOrder();
        int remaining = amount;
        for (int i = 0; i < handlers.size() && remaining > 0; i++) {
            remaining -= handlers.get(i).extractEnergy(remaining, simulate);
        }
        return amount - remaining;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(network.energy().stored(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(network.energy().capacity(), Integer.MAX_VALUE);
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
