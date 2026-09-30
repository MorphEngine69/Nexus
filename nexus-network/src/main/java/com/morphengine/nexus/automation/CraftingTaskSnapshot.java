package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Everything a crafting task needs to carry on after the world is loaded again.
 *
 * @param requester name of whoever asked for the task; empty when nobody in particular did
 * @param state     never {@link TaskState#PAUSED}, which only a status shows
 * @param held      what the task holds, gathered or crafted
 * @param toGather  what the task still has to take from the network's storage
 */
public record CraftingTaskSnapshot(
        UUID id, ResourceAmount target, String requester, TaskState state, List<ResourceAmount> held,
        List<ResourceAmount> toGather, List<BlueprintRunSnapshot> runs) {

    public CraftingTaskSnapshot {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(requester, "requester must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (state == TaskState.PAUSED) {
            throw new IllegalArgumentException("task " + id + " cannot be saved as paused");
        }
        held = List.copyOf(held);
        toGather = List.copyOf(toGather);
        runs = List.copyOf(runs);
    }
}
