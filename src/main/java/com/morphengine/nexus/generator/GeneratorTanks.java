package com.morphengine.nexus.generator;

import com.morphengine.nexus.block.entity.FluidKeeper;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The tanks of a generator, one for each {@link TankSpec}: each takes its own fluid, by bucket, by pipe or by
 * hand, and gives nothing out, since a generator is a place to burn fuel, not to keep it. What the generator burns it
 * draws out itself. Millibuckets; server thread only.
 */
public final class GeneratorTanks extends FluidStacksResourceHandler implements FluidKeeper {

    /** Millibuckets each tank holds. */
    public static final int CAPACITY_MILLIBUCKETS = GeneratorBalance.TANK_CAPACITY_MILLIBUCKETS;

    private final List<TankSpec> specs;
    private int capacityMillibuckets;
    private final Runnable changed;

    /**
     * @param specs    the tanks, in order
     * @param capacity millibuckets each tank holds
     * @param changed  called when the contents change
     */
    public GeneratorTanks(final List<TankSpec> specs, final int capacity, final Runnable changed) {
        super(NonNullList.withSize(specs.size(), FluidStack.EMPTY), capacity);
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

    public int capacityMillibuckets() {
        return capacityMillibuckets;
    }

    /**
     * @param millibuckets what each tank holds from now on; what a tank holds above it stays, and takes nothing more
     */
    public void setCapacityMillibuckets(final int millibuckets) {
        this.capacityMillibuckets = millibuckets;
        this.capacity = millibuckets;
    }

    @Override
    public boolean isValid(final int index, final FluidResource resource) {
        return specs.get(index).accepts(resource.getFluid());
    }

    @Override
    public int extract(
            final int index, final FluidResource resource, final int amount, final TransactionContext transaction) {
        return 0;
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
            final FluidStack before = stack.copy();
            stack.shrink(specs.get(index).portionMillibuckets());
            onContentsChanged(index, before);
        }
    }

    @Override
    protected void onContentsChanged(final int index, final FluidStack previousContents) {
        changed.run();
    }
}
