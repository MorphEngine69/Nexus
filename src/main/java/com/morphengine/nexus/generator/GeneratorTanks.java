package com.morphengine.nexus.generator;

import com.morphengine.nexus.block.entity.FluidKeeper;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.transfer.FluidResource;
import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The tanks of a generator, one for each {@link TankSpec}: each takes its own fluid, by bucket, by pipe or by
 * hand, and gives nothing out, since a generator is a place to burn fuel, not to keep it. What the generator burns it
 * draws out itself. Millibuckets; server thread only.
 */
public final class GeneratorTanks implements IFluidHandler, FluidKeeper {

    /** Millibuckets each tank holds. */
    public static final int CAPACITY_MILLIBUCKETS = GeneratorBalance.TANK_CAPACITY_MILLIBUCKETS;

    private static final String TANK_TAG = "tank";

    private final List<TankSpec> specs;
    private final NonNullList<FluidStack> stacks;
    private int capacityMillibuckets;
    private final Runnable changed;

    /**
     * @param specs    the tanks, in order
     * @param capacity millibuckets each tank holds
     * @param changed  called when the contents change
     */
    public GeneratorTanks(final List<TankSpec> specs, final int capacity, final Runnable changed) {
        this.stacks = NonNullList.withSize(specs.size(), FluidStack.EMPTY);
        this.specs = List.copyOf(specs);
        this.capacityMillibuckets = capacity;
        this.changed = changed;
    }

    /**
     * @return what the panel shows of each tank, in order
     */
    public List<TankView> views() {
        final List<TankView> views = new ArrayList<>(specs.size());
        for (int index = 0; index < specs.size(); index++) {
            final FluidResource fluid = getResource(index);
            views.add(new TankView(fluid.isEmpty() ? null : new FluidKey(fluid), getAmountAsInt(index),
                    capacityMillibuckets));
        }
        return views;
    }

    /**
     * @return the fluid in the tank {@code index}, empty when there is none
     */
    public FluidResource getResource(final int index) {
        return FluidResource.of(stacks.get(index));
    }

    /**
     * @return the millibuckets in the tank {@code index}
     */
    public int getAmountAsInt(final int index) {
        return stacks.get(index).getAmount();
    }

    /**
     * @return a copy of what each tank holds, in the order of the tanks
     */
    @Override
    public List<FluidStack> held() {
        final List<FluidStack> held = new ArrayList<>(specs.size());
        for (FluidStack stack : stacks) {
            held.add(stack.copy());
        }
        return held;
    }

    /**
     * Fills each tank with the fluid {@code held} has for it, as far as the tank takes that fluid and holds so much;
     * what does not fit is not kept.
     */
    @Override
    public void restore(final List<FluidStack> held) {
        for (int index = 0; index < specs.size(); index++) {
            final FluidStack stack = index < held.size() ? held.get(index).copy() : FluidStack.EMPTY;
            if (!stack.isEmpty() && !specs.get(index).accepts(stack.getFluid())) {
                continue;
            }
            stack.setAmount(Math.min(stack.getAmount(), capacityMillibuckets));
            stacks.set(index, stack);
        }
    }

    /**
     * Writes each tank under the name {@code tank<index>}.
     */
    public void save(final ValueOutput target) {
        for (int index = 0; index < stacks.size(); index++) {
            target.store(TANK_TAG + index, FluidStack.OPTIONAL_CODEC, stacks.get(index));
        }
    }

    /**
     * Reads what {@link #save} wrote; a tank that is not there is empty.
     */
    public void load(final ValueInput source) {
        final List<FluidStack> read = new ArrayList<>(stacks.size());
        for (int index = 0; index < stacks.size(); index++) {
            read.add(source.read(TANK_TAG + index, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY));
        }
        restore(read);
    }

    public int capacityMillibuckets() {
        return capacityMillibuckets;
    }

    /**
     * @param millibuckets what each tank holds from now on; what a tank holds above it stays, and takes nothing more
     */
    public void setCapacityMillibuckets(final int millibuckets) {
        this.capacityMillibuckets = millibuckets;
    }

    @Override
    public int getTanks() {
        return specs.size();
    }

    @Override
    public FluidStack getFluidInTank(final int tank) {
        return stacks.get(tank);
    }

    @Override
    public int getTankCapacity(final int tank) {
        return capacityMillibuckets;
    }

    @Override
    public boolean isFluidValid(final int tank, final FluidStack stack) {
        return specs.get(tank).accepts(stack.getFluid());
    }

    @Override
    public int fill(final FluidStack resource, final FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        for (int index = 0; index < specs.size(); index++) {
            if (!isFluidValid(index, resource)) {
                continue;
            }
            final FluidStack held = stacks.get(index);
            if (!held.isEmpty() && !FluidStack.isSameFluidSameComponents(held, resource)) {
                continue;
            }
            final int room = Math.max(0, capacityMillibuckets - held.getAmount());
            final int accepted = Math.min(room, resource.getAmount());
            if (accepted > 0 && action.execute()) {
                if (held.isEmpty()) {
                    stacks.set(index, resource.copyWithAmount(accepted));
                } else {
                    held.grow(accepted);
                }
                changed.run();
            }
            return accepted;
        }
        return 0;
    }

    @Override
    public FluidStack drain(final FluidStack resource, final FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(final int maxDrain, final FluidAction action) {
        return FluidStack.EMPTY;
    }

    /**
     * @return whether every tank holds what one portion of fuel takes
     */
    public boolean holdsPortion() {
        for (int index = 0; index < specs.size(); index++) {
            if (getAmountAsInt(index) < specs.get(index).portionMillibuckets()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes one portion of fuel out of the tanks.
     *
     * @throws IllegalStateException if a tank holds too little, see {@link #holdsPortion()}
     */
    public void takePortion() {
        if (!holdsPortion()) {
            throw new IllegalStateException("the tanks hold less than a portion of fuel");
        }
        for (int index = 0; index < specs.size(); index++) {
            final FluidStack stack = stacks.get(index);
            stack.shrink(specs.get(index).portionMillibuckets());
            if (stack.isEmpty()) {
                stacks.set(index, FluidStack.EMPTY);
            }
        }
        changed.run();
    }
}
