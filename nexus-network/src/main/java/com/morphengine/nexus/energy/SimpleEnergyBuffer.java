package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;

import java.util.Objects;

/**
 * An {@link EnergyBuffer} with a fixed capacity and per-operation rate limits.
 */
public final class SimpleEnergyBuffer implements EnergyBuffer {

    private final long capacity;
    private final long maxInsert;
    private final long maxExtract;
    private long stored;
    private long totalInserted;
    private long totalExtracted;

    /**
     * @param capacity   FE the buffer holds, must be positive
     * @param maxInsert  FE accepted per insert call, must not be negative
     * @param maxExtract FE removed per extract call, must not be negative
     */
    public SimpleEnergyBuffer(final long capacity, final long maxInsert, final long maxExtract) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        this.maxInsert = requireNotNegative(maxInsert, "maxInsert");
        this.maxExtract = requireNotNegative(maxExtract, "maxExtract");
    }

    @Override
    public long stored() {
        return stored;
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public long insert(final long amount, final Action action) {
        requireNotNegative(amount, "amount");
        Objects.requireNonNull(action, "action must not be null");
        final long accepted = Math.min(amount, Math.min(maxInsert, capacity - stored));
        if (action.isExecute()) {
            stored += accepted;
            totalInserted += accepted;
        }
        return accepted;
    }

    @Override
    public long extract(final long amount, final Action action) {
        requireNotNegative(amount, "amount");
        Objects.requireNonNull(action, "action must not be null");
        final long removed = Math.min(amount, Math.min(maxExtract, stored));
        if (action.isExecute()) {
            stored -= removed;
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

    public Snapshot snapshot() {
        return new Snapshot(stored, totalInserted, totalExtracted);
    }

    /**
     * Returns the buffer to a previously taken or loaded state. A stored amount
     * above the capacity is cut down to the capacity.
     */
    public void restore(final Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        stored = Math.min(snapshot.stored(), capacity);
        totalInserted = snapshot.totalInserted();
        totalExtracted = snapshot.totalExtracted();
    }

    private static long requireNotNegative(final long value, final String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
        return value;
    }

    /**
     * Complete state of a {@link SimpleEnergyBuffer}, all values in FE.
     */
    public record Snapshot(long stored, long totalInserted, long totalExtracted) {

        public Snapshot {
            requireNotNegative(stored, "stored");
            requireNotNegative(totalInserted, "totalInserted");
            requireNotNegative(totalExtracted, "totalExtracted");
        }

        public static Snapshot storing(final long stored) {
            return new Snapshot(stored, 0, 0);
        }
    }
}
