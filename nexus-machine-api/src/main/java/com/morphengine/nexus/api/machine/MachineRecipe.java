package com.morphengine.nexus.api.machine;

import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.List;
import java.util.Objects;

/**
 * What a machine does to the resources in a line: takes the inputs, works for a time on FE, and gives the output.
 * Immutable.
 *
 * @param inputs        what is used up when the work is done, at least one entry, each resource once
 * @param output        what the work gives
 * @param ticks         game ticks the work takes at a speed of 100 percent, positive
 * @param energyPerTick FE the work takes in each of those ticks at a speed of 100 percent, positive; a faster machine
 *                      takes proportionally more in each tick and so the same in all
 */
public record MachineRecipe(List<ResourceAmount> inputs, ResourceAmount output, int ticks, long energyPerTick) {

    public MachineRecipe {
        Objects.requireNonNull(output, "output must not be null");
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs must not be null"));
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("a recipe for " + output + " needs at least one input");
        }
        if (inputs.stream().map(ResourceAmount::resource).distinct().count() != inputs.size()) {
            throw new IllegalArgumentException("a resource is listed twice among the inputs " + inputs);
        }
        if (ticks <= 0) {
            throw new IllegalArgumentException("ticks of the recipe for " + output + " must be positive: " + ticks);
        }
        if (energyPerTick <= 0) {
            throw new IllegalArgumentException(
                    "energyPerTick of the recipe for " + output + " must be positive: " + energyPerTick);
        }
    }

    /**
     * @return a recipe with the single input that most machines have
     */
    public static MachineRecipe of(
            final ResourceAmount input, final ResourceAmount output, final int ticks, final long energyPerTick) {
        return new MachineRecipe(List.of(input), output, ticks, energyPerTick);
    }
}
