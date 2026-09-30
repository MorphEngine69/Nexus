package com.morphengine.nexus.api.automation;

import java.util.Objects;

/**
 * How many runs of one blueprint a crafting plan needs.
 *
 * @param runs number of runs, always positive
 */
public record PlannedRuns(Blueprint blueprint, long runs) {

    public PlannedRuns {
        Objects.requireNonNull(blueprint, "blueprint must not be null");
        if (runs <= 0) {
            throw new IllegalArgumentException("runs of " + blueprint + " must be positive: " + runs);
        }
    }
}
