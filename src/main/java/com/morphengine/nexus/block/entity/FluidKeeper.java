package com.morphengine.nexus.block.entity;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * The tanks of a device, as far as taking it down and putting it up again is concerned. Millibuckets; server thread
 * only.
 */
public interface FluidKeeper {

    /**
     * @return a copy of what each tank holds, in the order of the tanks
     */
    List<FluidStack> held();

    /**
     * Fills each tank with what {@code fluids} has for it, as far as the tank takes that fluid and holds so much; what
     * does not fit is not kept.
     */
    void restore(List<FluidStack> fluids);
}
