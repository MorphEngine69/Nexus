package com.morphengine.nexus.api.automation;

import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.Objects;

/**
 * One resource of a crafting task, as a crafting monitor shows it. Every
 * amount is zero or more.
 *
 * @param held       units the task holds, gathered or already crafted
 * @param scheduled  units runs not yet handed out will give
 * @param processing units runs handed out give but have not returned yet
 * @param problem    why the runs giving this resource do not go on;
 *                   {@link DispatchResult#ACCEPTED} when nothing holds them up
 */
public record TaskEntry(ResourceKey resource, long held, long scheduled, long processing, DispatchResult problem) {

    public TaskEntry {
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(problem, "problem must not be null");
        if (held < 0 || scheduled < 0 || processing < 0) {
            throw new IllegalArgumentException("amounts of " + resource + " must not be negative: held=" + held
                    + ", scheduled=" + scheduled + ", processing=" + processing);
        }
    }
}
