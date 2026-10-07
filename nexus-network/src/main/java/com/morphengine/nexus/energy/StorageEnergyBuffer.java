package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.math.SaturatedMath;

import java.util.Objects;

/**
 * The energy a {@link Storage} holds as one of its resources, such as FE in the
 * energy cells of a network's vaults, seen as an {@link EnergyBuffer}. It has no
 * rate limits of its own; the storage decides how much fits. Only what moves
 * through this buffer is counted in its totals. Server thread only.
 */
public final class StorageEnergyBuffer implements EnergyBuffer {

    private final Storage storage;
    private final ResourceKey energy;
    private final Actor actor;
    private long totalInserted;
    private long totalExtracted;

    /**
     * @param storage shared with its owner and read live
     * @param energy  the resource of the storage that is energy
     * @param actor   who the operations on the storage are performed for
     */
    public StorageEnergyBuffer(final Storage storage, final ResourceKey energy, final Actor actor) {
        this.storage = Objects.requireNonNull(storage, "storage must not be null");
        this.energy = Objects.requireNonNull(energy, "energy must not be null");
        this.actor = Objects.requireNonNull(actor, "actor must not be null");
    }

    @Override
    public long stored() {
        return storage.amountOf(energy);
    }

    /**
     * @return what is stored plus what still fits, asked of the storage each time
     */
    @Override
    public long capacity() {
        return SaturatedMath.add(stored(), storage.insert(energy, Long.MAX_VALUE, Action.SIMULATE, actor));
    }

    @Override
    public long insert(final long amount, final Action action) {
        requireNotNegative(amount);
        if (amount == 0) {
            return 0;
        }
        final long accepted = storage.insert(energy, amount, action, actor);
        if (action.isExecute()) {
            totalInserted += accepted;
        }
        return accepted;
    }

    @Override
    public long extract(final long amount, final Action action) {
        requireNotNegative(amount);
        if (amount == 0) {
            return 0;
        }
        final long removed = storage.extract(energy, amount, action, actor);
        if (action.isExecute()) {
            totalExtracted += removed;
        }
        return removed;
    }

    @Override
    public long totalInserted() {
        return totalInserted;
    }

    @Override
    public long totalExtracted() {
        return totalExtracted;
    }

    private static void requireNotNegative(final long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
    }
}
