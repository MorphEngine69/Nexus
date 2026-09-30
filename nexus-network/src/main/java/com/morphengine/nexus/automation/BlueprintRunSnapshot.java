package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.List;
import java.util.Objects;

/**
 * The saved progress of one blueprint within a crafting task.
 *
 * @param totalRuns  runs the plan asked for, always positive
 * @param toDispatch runs not handed to an executor yet, from zero to {@code totalRuns}
 * @param awaited    outputs not received yet, of runs handed out or still to hand out
 */
public record BlueprintRunSnapshot(Blueprint blueprint, long totalRuns, long toDispatch,
                                   List<ResourceAmount> awaited) {

    public BlueprintRunSnapshot {
        Objects.requireNonNull(blueprint, "blueprint must not be null");
        if (totalRuns <= 0 || toDispatch < 0 || toDispatch > totalRuns) {
            throw new IllegalArgumentException("runs of " + blueprint + " out of range: total=" + totalRuns
                    + ", to dispatch=" + toDispatch);
        }
        awaited = List.copyOf(awaited);
    }
}
