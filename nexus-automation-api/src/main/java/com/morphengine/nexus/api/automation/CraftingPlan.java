package com.morphengine.nexus.api.automation;

import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.List;
import java.util.Objects;

/**
 * What crafting an amount of a resource takes, worked out before anything
 * moves: the runs of each blueprint, what comes out of the network's storage,
 * what gets crafted along the way, and what is missing.
 *
 * @param target      the resource asked for and how much of it
 * @param runs        runs per blueprint, blueprints whose outputs others need first
 * @param fromStorage what is taken from the network's storage when the task starts
 * @param crafted     everything the runs give, the target included
 * @param missing     what neither the storage holds nor any blueprint gives;
 *                    a plan with anything missing cannot start
 */
public record CraftingPlan(
        ResourceAmount target,
        List<PlannedRuns> runs,
        List<ResourceAmount> fromStorage,
        List<ResourceAmount> crafted,
        List<ResourceAmount> missing) {

    public CraftingPlan {
        Objects.requireNonNull(target, "target must not be null");
        runs = List.copyOf(runs);
        fromStorage = List.copyOf(fromStorage);
        crafted = List.copyOf(crafted);
        missing = List.copyOf(missing);
    }

    /**
     * @return whether the plan can start: nothing is missing and at least one blueprint runs
     */
    public boolean isComplete() {
        return missing.isEmpty() && !runs.isEmpty();
    }
}
