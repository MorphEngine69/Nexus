package com.morphengine.nexus.processing;

import com.morphengine.nexus.machine.MachineSlot;
import com.morphengine.nexus.machine.MachineSlots;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.LongSupplier;

/**
 * The tank of a machine that gives a fluid, such as an Extractor: what the line of the machine puts in it, what a
 * pipe or a Puller takes out through a side that lets things out, and what is shown and saved. It takes nothing in
 * from outside. Millibuckets; server thread only.
 */
public final class MachineTank {

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
        this.output = new Output(stacks, changed);
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
    public ResourceHandler<FluidResource> handler() {
        output.setCapacity((int) capacity.getAsLong());
        return output;
    }

    public TankView view() {
        final FluidStack stack = stacks.getFirst();
        return new TankView(stack.isEmpty() ? null : new FluidKey(FluidResource.of(stack)), stack.getAmount(),
                capacity.getAsLong());
    }

    public void save(final ValueOutput target) {
        target.store(TAG_TANK, FluidStack.OPTIONAL_CODEC, stacks.getFirst());
    }

    public void load(final ValueInput source) {
        stacks.set(0, source.read(TAG_TANK, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY));
    }

    /**
     * The stacks of the tank as a handler of the game. The superclass copies the list it is given, so the constructor
     * puts the original back: the stacks are what the line works on.
     */
    private static final class Output extends FluidStacksResourceHandler {

        private final Runnable changed;

        Output(final NonNullList<FluidStack> stacks, final Runnable changed) {
            super(stacks, 0);
            this.stacks = stacks;
            this.changed = changed;
        }

        void setCapacity(final int millibuckets) {
            this.capacity = millibuckets;
        }

        @Override
        public boolean isValid(final int index, final FluidResource resource) {
            return false;
        }

        @Override
        public int insert(
                final int index, final FluidResource resource, final int amount,
                final TransactionContext transaction) {
            return 0;
        }

        @Override
        protected void onContentsChanged(final int index, final FluidStack previousContents) {
            changed.run();
        }
    }
}
