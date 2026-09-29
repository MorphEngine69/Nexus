package com.morphengine.nexus.level;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The energy in the cells of a network's storage as a transaction-aware
 * {@link EnergyHandler}. Changes apply at once; an aborted transaction moves the
 * difference back, which restores the total though not necessarily which cell
 * held it. Server thread only.
 */
final class StoredEnergyHandler extends SnapshotJournal<Long> implements EnergyHandler {

    private final EnergyBuffer stored;

    /**
     * @param stored the storage's energy, shared with the network's pool
     */
    StoredEnergyHandler(final EnergyBuffer stored) {
        this.stored = stored;
    }

    @Override
    public long getAmountAsLong() {
        return stored.stored();
    }

    @Override
    public long getCapacityAsLong() {
        return stored.capacity();
    }

    @Override
    public int insert(final int amount, final TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        final long accepted = stored.insert(amount, Action.SIMULATE);
        if (accepted > 0) {
            updateSnapshots(transaction);
            stored.insert(accepted, Action.EXECUTE);
        }
        return (int) accepted;
    }

    @Override
    public int extract(final int amount, final TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        final long removed = stored.extract(amount, Action.SIMULATE);
        if (removed > 0) {
            updateSnapshots(transaction);
            stored.extract(removed, Action.EXECUTE);
        }
        return (int) removed;
    }

    @Override
    protected Long createSnapshot() {
        return stored.stored();
    }

    @Override
    protected void revertToSnapshot(final Long snapshot) {
        final long difference = stored.stored() - snapshot;
        if (difference > 0) {
            stored.extract(difference, Action.EXECUTE);
        } else if (difference < 0) {
            stored.insert(-difference, Action.EXECUTE);
        }
    }
}
