package com.morphengine.nexus.automation;

/**
 * What a plan is asked for.
 */
public enum PlanGoal {

    /** The amount asked for, whether or not it can be crafted. */
    EXACT,

    /** The most of it, up to the amount asked for, that can be crafted with nothing missing. */
    LARGEST
}
