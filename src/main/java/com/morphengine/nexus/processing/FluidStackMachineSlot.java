package com.morphengine.nexus.processing;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.machine.MachineSlot;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.transfer.FluidResource;
import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * A {@link MachineSlot} that is a tank: the fluid in it is one of the stacks of a list, which a handler of the game
 * shares, so what the handler changes the slot sees and the other way round. Holds fluids only; amounts are
 * millibuckets. Server thread only.
 */
final class FluidStackMachineSlot implements MachineSlot {

    private final NonNullList<FluidStack> tanks;
    private final int index;
    private final LongSupplier capacity;
    private final Runnable changed;

    /**
     * @param capacity millibuckets the tank holds, asked for each time as it grows with the tier
     * @param changed  called when the contents change
     */
    FluidStackMachineSlot(
            final NonNullList<FluidStack> tanks, final int index, final LongSupplier capacity,
            final Runnable changed) {
        this.tanks = Objects.requireNonNull(tanks, "tanks must not be null");
        this.index = index;
        this.capacity = Objects.requireNonNull(capacity, "capacity must not be null");
        this.changed = Objects.requireNonNull(changed, "changed must not be null");
    }

    @Override
    public @Nullable ResourceKey resource() {
        final FluidStack stack = tanks.get(index);
        return stack.isEmpty() ? null : new FluidKey(FluidResource.of(stack));
    }

    @Override
    public long amount() {
        return tanks.get(index).getAmount();
    }

    @Override
    public long room(final ResourceKey wanted) {
        if (!(wanted instanceof FluidKey key)) {
            return 0;
        }
        final FluidStack stack = tanks.get(index);
        if (stack.isEmpty()) {
            return capacity.getAsLong();
        }
        return key.fluid().matches(stack) ? Math.max(0, capacity.getAsLong() - stack.getAmount()) : 0;
    }

    @Override
    public long insert(final ResourceKey inserted, final long offered, final Action action) {
        requirePositive(offered);
        final long accepted = Math.min(offered, room(inserted));
        if (accepted > 0 && action.isExecute()) {
            final FluidStack stack = tanks.get(index);
            if (stack.isEmpty()) {
                tanks.set(index, ((FluidKey) inserted).toStack((int) accepted));
            } else {
                stack.grow((int) accepted);
            }
            changed.run();
        }
        return accepted;
    }

    @Override
    public long extract(final ResourceKey extracted, final long requested, final Action action) {
        requirePositive(requested);
        final FluidStack stack = tanks.get(index);
        if (stack.isEmpty() || !(extracted instanceof FluidKey key) || !key.fluid().matches(stack)) {
            return 0;
        }
        final long removed = Math.min(requested, stack.getAmount());
        if (removed > 0 && action.isExecute()) {
            stack.shrink((int) removed);
            if (stack.isEmpty()) {
                tanks.set(index, FluidStack.EMPTY);
            }
            changed.run();
        }
        return removed;
    }

    @Override
    public void restore(final @Nullable ResourceKey saved, final long savedAmount) {
        if (saved instanceof FluidKey key && savedAmount > 0) {
            tanks.set(index, key.toStack((int) Math.min(savedAmount, capacity.getAsLong())));
        } else {
            tanks.set(index, FluidStack.EMPTY);
        }
    }

    private static void requirePositive(final long units) {
        if (units <= 0) {
            throw new IllegalArgumentException("units must be positive: " + units);
        }
    }
}
