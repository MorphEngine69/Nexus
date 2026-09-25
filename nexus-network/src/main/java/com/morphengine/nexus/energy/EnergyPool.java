package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;

import java.util.List;
import java.util.Objects;

/**
 * The energy of one network: its buffers seen as a single {@link EnergyBuffer}.
 * Inserts fill and extracts drain the buffers in list order. Sums saturate at
 * {@link Long#MAX_VALUE} instead of overflowing.
 */
public final class EnergyPool implements EnergyBuffer {

    public static final EnergyPool EMPTY = new EnergyPool(List.of());

    private final List<EnergyBuffer> buffers;

    /**
     * @param buffers buffers in fill order; the list is copied, the buffers are
     *                shared with their owners and read live
     */
    public EnergyPool(final List<? extends EnergyBuffer> buffers) {
        this.buffers = List.copyOf(Objects.requireNonNull(buffers, "buffers must not be null"));
    }

    public int size() {
        return buffers.size();
    }

    @Override
    public long stored() {
        long total = 0;
        for (EnergyBuffer buffer : buffers) {
            total = saturatedAdd(total, buffer.stored());
        }
        return total;
    }

    @Override
    public long capacity() {
        long total = 0;
        for (EnergyBuffer buffer : buffers) {
            total = saturatedAdd(total, buffer.capacity());
        }
        return total;
    }

    @Override
    public long insert(final long amount, final Action action) {
        requireNotNegative(amount);
        long remaining = amount;
        for (int i = 0; i < buffers.size() && remaining > 0; i++) {
            remaining -= buffers.get(i).insert(remaining, action);
        }
        return amount - remaining;
    }

    @Override
    public long extract(final long amount, final Action action) {
        requireNotNegative(amount);
        long remaining = amount;
        for (int i = 0; i < buffers.size() && remaining > 0; i++) {
            remaining -= buffers.get(i).extract(remaining, action);
        }
        return amount - remaining;
    }

    @Override
    public long totalInserted() {
        long total = 0;
        for (EnergyBuffer buffer : buffers) {
            total = saturatedAdd(total, buffer.totalInserted());
        }
        return total;
    }

    @Override
    public long totalExtracted() {
        long total = 0;
        for (EnergyBuffer buffer : buffers) {
            total = saturatedAdd(total, buffer.totalExtracted());
        }
        return total;
    }

    private static long saturatedAdd(final long left, final long right) {
        final long sum = left + right;
        return sum < left ? Long.MAX_VALUE : sum;
    }

    private static void requireNotNegative(final long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
    }
}
