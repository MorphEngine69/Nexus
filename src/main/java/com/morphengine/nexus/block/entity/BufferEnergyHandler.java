package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a {@link SimpleEnergyBuffer} through NeoForge's transactional energy
 * API. Changes are applied at once and undone from a snapshot if the
 * transaction is aborted; {@code onCommit} runs when the outermost transaction
 * commits.
 */
final class BufferEnergyHandler extends SnapshotJournal<SimpleEnergyBuffer.Snapshot> implements EnergyHandler {

    private final SimpleEnergyBuffer buffer;
    private final Access access;
    private final Runnable onCommit;

    BufferEnergyHandler(final SimpleEnergyBuffer buffer, final Access access, final Runnable onCommit) {
        this.buffer = buffer;
        this.access = access;
        this.onCommit = onCommit;
    }

    @Override
    public long getAmountAsLong() {
        return buffer.stored();
    }

    @Override
    public long getCapacityAsLong() {
        return buffer.capacity();
    }

    @Override
    public int insert(final int amount, final TransactionContext transaction) {
        if (access == Access.GIVE_ONLY) {
            return 0;
        }
        final long accepted = buffer.insert(amount, Action.SIMULATE);
        if (accepted > 0) {
            updateSnapshots(transaction);
            buffer.insert(accepted, Action.EXECUTE);
        }
        return (int) accepted;
    }

    @Override
    public int extract(final int amount, final TransactionContext transaction) {
        if (access == Access.RECEIVE_ONLY) {
            return 0;
        }
        final long removed = buffer.extract(amount, Action.SIMULATE);
        if (removed > 0) {
            updateSnapshots(transaction);
            buffer.extract(removed, Action.EXECUTE);
        }
        return (int) removed;
    }

    @Override
    protected SimpleEnergyBuffer.Snapshot createSnapshot() {
        return buffer.snapshot();
    }

    @Override
    protected void revertToSnapshot(final SimpleEnergyBuffer.Snapshot snapshot) {
        buffer.restore(snapshot);
    }

    @Override
    protected void onRootCommit(final SimpleEnergyBuffer.Snapshot originalState) {
        onCommit.run();
    }

    /**
     * What other blocks may do with the buffer through this handler.
     */
    enum Access {
        RECEIVE_AND_GIVE,
        /** Energy is only taken out; the owner fills the buffer itself. */
        GIVE_ONLY,
        /** Energy is only put in; the owner is the one that takes it out. */
        RECEIVE_ONLY
    }
}
