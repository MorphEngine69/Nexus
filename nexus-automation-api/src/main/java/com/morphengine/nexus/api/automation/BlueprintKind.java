package com.morphengine.nexus.api.automation;

/**
 * How a blueprint is run.
 */
public enum BlueprintKind {

    /** A crafting recipe the executor crafts itself, at once. */
    CRAFTING,

    /** Processing in a machine: the inputs go into it, the outputs come back later. */
    PROCESSING
}
