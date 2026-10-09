package com.morphengine.nexus.gametest;

import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

/**
 * An energy storage whose changes belong to a {@link Transaction}.
 */
final class EnergyHandler {

    private final IEnergyStorage storage;

    private EnergyHandler(final IEnergyStorage storage) {
        this.storage = storage;
    }

    static @Nullable EnergyHandler of(final @Nullable IEnergyStorage storage) {
        return storage == null ? null : new EnergyHandler(storage);
    }

    int insert(final int amount, final Transaction transaction) {
        final int accepted = storage.receiveEnergy(amount, false);
        transaction.onRollback(() -> storage.extractEnergy(accepted, false));
        return accepted;
    }

    int extract(final int amount, final Transaction transaction) {
        final int removed = storage.extractEnergy(amount, false);
        transaction.onRollback(() -> storage.receiveEnergy(removed, false));
        return removed;
    }
}
