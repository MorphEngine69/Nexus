package com.morphengine.nexus.api.core;

/**
 * Whether an operation only reports its outcome or actually changes state.
 */
public enum Action {
    SIMULATE,
    EXECUTE;

    public boolean isExecute() {
        return this == EXECUTE;
    }
}
