package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.math.SaturatedMath;

import java.util.List;
import java.util.Objects;

/**
 * The energy of one network: its buffers seen as a single {@link EnergyBuffer}.
 * Inserts fill the buffers in the {@linkplain PriorityOrder#fillOrder fill
 * order} of their priorities, extracts drain them in the {@linkplain
 * PriorityOrder#drainOrder drain order}. Sums saturate at {@link Long#MAX_VALUE}
 * instead of overflowing.
 */
public final class EnergyPool implements EnergyBuffer {

    public static final EnergyPool EMPTY = new EnergyPool(List.of());

    private final List<EnergyBuffer> buffers;
    private final List<EnergyBuffer> drainOrder;

    /**
     * @param buffers buffers all at one priority, filled and drained in list
     *                order; the list is copied, the buffers are shared with
     *                their owners and read live
     */
    public EnergyPool(final List<? extends EnergyBuffer> buffers) {
        this(PriorityOrder.inListOrder(Objects.requireNonNull(buffers, "buffers must not be null")));
    }

    /**
     * @param buffers the buffers by priority; shared with their owners and read live
     */
    public EnergyPool(final PriorityOrder<EnergyBuffer> buffers) {
        Objects.requireNonNull(buffers, "buffers must not be null");
        this.buffers = buffers.fillOrder();
        this.drainOrder = buffers.drainOrder();
    }

    public int size() {
        return buffers.size();
    }

    @Override
    public long stored() {
        long total = 0;
        for (EnergyBuffer buffer : buffers) {
            total = SaturatedMath.add(total, buffer.stored());
        }
        return total;
    }

    @Override
    public long capacity() {
        long total = 0;
        for (EnergyBuffer buffer : buffers) {
            total = SaturatedMath.add(total, buffer.capacity());
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
        for (int i = 0; i < drainOrder.size() && remaining > 0; i++) {
            remaining -= drainOrder.get(i).extract(remaining, action);
        }
        return amount - remaining;
    }

    @Override
    public long totalInserted() {
        long total = 0;
        for (EnergyBuffer buffer : buffers) {
            total = SaturatedMath.add(total, buffer.totalInserted());
        }
        return total;
    }

    @Override
    public long totalExtracted() {
        long total = 0;
        for (EnergyBuffer buffer : buffers) {
            total = SaturatedMath.add(total, buffer.totalExtracted());
        }
        return total;
    }

    private static void requireNotNegative(final long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
    }
}
