package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Exposes a {@link SimpleEnergyBuffer} as an energy storage of the game. Changes are applied at once;
 * {@code onCommit} runs when energy actually moved.
 */
final class BufferEnergyHandler implements IEnergyStorage {

    private final SimpleEnergyBuffer buffer;
    private final Access access;
    private final Runnable onCommit;

    BufferEnergyHandler(final SimpleEnergyBuffer buffer, final Access access, final Runnable onCommit) {
        this.buffer = buffer;
        this.access = access;
        this.onCommit = onCommit;
    }

    @Override
    public int receiveEnergy(final int amount, final boolean simulate) {
        if (access == Access.GIVE_ONLY || amount <= 0) {
            return 0;
        }
        final long accepted = buffer.insert(amount, simulate ? Action.SIMULATE : Action.EXECUTE);
        if (accepted > 0 && !simulate) {
            onCommit.run();
        }
        return (int) accepted;
    }

    @Override
    public int extractEnergy(final int amount, final boolean simulate) {
        if (access == Access.RECEIVE_ONLY || amount <= 0) {
            return 0;
        }
        final long removed = buffer.extract(amount, simulate ? Action.SIMULATE : Action.EXECUTE);
        if (removed > 0 && !simulate) {
            onCommit.run();
        }
        return (int) removed;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(buffer.stored(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(buffer.capacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() {
        return access != Access.RECEIVE_ONLY;
    }

    @Override
    public boolean canReceive() {
        return access != Access.GIVE_ONLY;
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
