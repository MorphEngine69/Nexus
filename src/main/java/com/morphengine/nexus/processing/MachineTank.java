package com.morphengine.nexus.processing;

import com.morphengine.nexus.block.entity.FluidKeeper;
import com.morphengine.nexus.machine.MachineSlot;
import com.morphengine.nexus.machine.MachineSlots;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.transfer.FluidResource;
import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * The tank of a machine that gives a fluid, such as an Extractor: what the line of the machine puts in it, what a
 * pipe or a Puller takes out through a side that lets things out, and what is shown and saved. It takes nothing in
 * from outside. Millibuckets; server thread only.
 */
public final class MachineTank implements FluidKeeper {

    private static final String TAG_TANK = "tank";

    private final NonNullList<FluidStack> stacks = NonNullList.withSize(1, FluidStack.EMPTY);
    private final LongSupplier capacity;
    private final Runnable changed;
    private final Output output;

    /**
     * @param capacity millibuckets the tank holds, asked for each time as it grows with the tier
     * @param changed  called when the contents change
     */
    public MachineTank(final LongSupplier capacity, final Runnable changed) {
        this.capacity = capacity;
        this.changed = changed;
        this.output = new Output(stacks, capacity, changed);
    }

    /**
     * @return slots for a machine: the input slots of {@code inputs}, and the tank as the output of its only line
     */
    public MachineSlots slotsOver(final ItemStackSlots inputs) {
        final MachineSlot slot = new FluidStackMachineSlot(stacks, 0, capacity, changed);
        return new MachineSlots() {
            @Override
            public MachineSlot input(final int index) {
                return inputs.input(index);
            }

            @Override
            public MachineSlot output(final int index) {
                return slot;
            }
        };
    }

    /**
     * @return what a pipe sees of the tank: its fluid to take out, nothing to put in
     */
    public IFluidHandler handler() {
        return output;
    }

    public TankView view() {
        final FluidStack stack = stacks.getFirst();
        return new TankView(stack.isEmpty() ? null : new FluidKey(FluidResource.of(stack)), stack.getAmount(),
                capacity.getAsLong());
    }

    @Override
    public List<FluidStack> held() {
        return List.of(stacks.getFirst().copy());
    }

    @Override
    public void restore(final List<FluidStack> fluids) {
        final FluidStack kept = fluids.isEmpty() ? FluidStack.EMPTY : fluids.getFirst().copy();
        kept.setAmount((int) Math.min(kept.getAmount(), capacity.getAsLong()));
        stacks.set(0, kept);
    }

    public void save(final ValueOutput target) {
        target.store(TAG_TANK, FluidStack.OPTIONAL_CODEC, stacks.getFirst());
    }

    public void load(final ValueInput source) {
        stacks.set(0, source.read(TAG_TANK, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY));
    }

    /**
     * The tank as a handler of the game, which only gives its fluid out. The stacks are what the line works on.
     */
    private static final class Output implements IFluidHandler {

        private final NonNullList<FluidStack> stacks;
        private final LongSupplier capacity;
        private final Runnable changed;

        Output(final NonNullList<FluidStack> stacks, final LongSupplier capacity, final Runnable changed) {
            this.stacks = stacks;
            this.capacity = capacity;
            this.changed = changed;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return stacks.get(0);
        }

        @Override
        public int getTankCapacity(final int tank) {
            return (int) capacity.getAsLong();
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return false;
        }

        @Override
        public int fill(final FluidStack resource, final FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidAction action) {
            final FluidStack held = stacks.get(0);
            return FluidStack.isSameFluidSameComponents(held, resource) ? drain(resource.getAmount(), action)
                    : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidAction action) {
            final FluidStack held = stacks.get(0);
            if (held.isEmpty() || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            final int drained = Math.min(maxDrain, held.getAmount());
            final FluidStack result = held.copyWithAmount(drained);
            if (action.execute()) {
                held.shrink(drained);
                if (held.isEmpty()) {
                    stacks.set(0, FluidStack.EMPTY);
                }
                changed.run();
            }
            return result;
        }
    }
}
