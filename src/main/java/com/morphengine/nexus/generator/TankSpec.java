package com.morphengine.nexus.generator;

import net.minecraft.world.level.material.Fluid;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A tank of a generator: the one fluid it takes and how much of it a portion of fuel uses.
 *
 * @param fluid                  the fluid the tank takes, asked for each time since a fluid of the mod does not exist
 *                               yet when the kinds of generator are set up; its flowing form counts as well
 * @param portionMillibuckets    millibuckets of the fluid in one portion of fuel, positive
 */
public record TankSpec(Supplier<? extends Fluid> fluid, int portionMillibuckets) {

    public TankSpec {
        Objects.requireNonNull(fluid, "fluid");
        if (portionMillibuckets <= 0) {
            throw new IllegalArgumentException("a portion needs a positive amount: " + portionMillibuckets);
        }
    }

    public boolean accepts(final Fluid candidate) {
        return candidate.isSame(fluid.get());
    }
}
