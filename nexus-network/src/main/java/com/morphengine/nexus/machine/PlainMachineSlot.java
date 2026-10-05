package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.ToLongFunction;

/**
 * A {@link MachineSlot} that keeps what it holds itself, up to a limit that depends on the resource, such as the stack
 * size of an item. Server thread only.
 */
public final class PlainMachineSlot implements MachineSlot {

    private final ToLongFunction<ResourceKey> limit;
    private @Nullable ResourceKey resource;
    private long amount;

    /**
     * @param limit how many units of a resource the slot holds at most
     */
    public PlainMachineSlot(final ToLongFunction<ResourceKey> limit) {
        this.limit = Objects.requireNonNull(limit, "limit must not be null");
    }

    /**
     * @return what the slot holds, {@code null} when it is empty
     */
    @Override
    public @Nullable ResourceKey resource() {
        return resource;
    }

    @Override
    public long amount() {
        return amount;
    }

    @Override
    public boolean isEmpty() {
        return resource == null;
    }

    /**
     * @return what the slot holds with the amount, empty when it is empty
     */
    @Override
    public Optional<ResourceAmount> contents() {
        return resource == null ? Optional.empty() : Optional.of(new ResourceAmount(resource, amount));
    }

    /**
     * @return how many units of {@code wanted} the slot takes now: the limit less what is there when it holds
     *         {@code wanted}, the whole limit when it is empty, zero when it holds something else
     */
    @Override
    public long room(final ResourceKey wanted) {
        Objects.requireNonNull(wanted, "wanted must not be null");
        if (resource != null && !resource.equals(wanted)) {
            return 0;
        }
        return Math.max(0, limit.applyAsLong(wanted) - amount);
    }

    /**
     * @param offered units offered, must be positive
     * @return units accepted, never more than {@link #room}; under {@link Action#SIMULATE} the units that would be
     */
    @Override
    public long insert(final ResourceKey inserted, final long offered, final Action action) {
        requirePositive(offered);
        final long accepted = Math.min(offered, room(inserted));
        if (accepted > 0 && action.isExecute()) {
            resource = inserted;
            amount += accepted;
        }
        return accepted;
    }

    /**
     * @param requested units wanted, must be positive
     * @return units removed, none when the slot holds another resource; under {@link Action#SIMULATE} the units that
     *         would be
     */
    @Override
    public long extract(final ResourceKey extracted, final long requested, final Action action) {
        requirePositive(requested);
        if (resource == null || !resource.equals(extracted)) {
            return 0;
        }
        final long removed = Math.min(requested, amount);
        if (action.isExecute()) {
            amount -= removed;
            if (amount == 0) {
                resource = null;
            }
        }
        return removed;
    }

    /**
     * Puts saved contents back; an amount of zero or less empties the slot.
     */
    @Override
    public void restore(final @Nullable ResourceKey saved, final long savedAmount) {
        if (saved == null || savedAmount <= 0) {
            resource = null;
            amount = 0;
            return;
        }
        resource = saved;
        amount = savedAmount;
    }

    private static void requirePositive(final long units) {
        if (units <= 0) {
            throw new IllegalArgumentException("units must be positive: " + units);
        }
    }
}
