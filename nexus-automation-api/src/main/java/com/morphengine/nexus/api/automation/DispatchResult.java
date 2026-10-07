package com.morphengine.nexus.api.automation;

/**
 * What became of an attempt to hand the inputs of one run to an executor.
 */
public enum DispatchResult {

    /** The executor took every input; the outputs will follow. */
    ACCEPTED,

    /** The executor has nothing to run the blueprint in, such as a machine that is gone. */
    NO_TARGET,

    /** The target cannot take all the inputs now; nothing was taken. */
    TARGET_FULL,

    /** The executor waits for an earlier run to finish before it takes another. */
    LOCKED,

    /** The network holds too little energy to pay for the run; nothing was taken. */
    NO_ENERGY;

    public boolean isAccepted() {
        return this == ACCEPTED;
    }
}
