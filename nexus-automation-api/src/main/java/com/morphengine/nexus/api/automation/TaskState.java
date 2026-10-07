package com.morphengine.nexus.api.automation;

/**
 * Where a crafting task is in its life.
 */
public enum TaskState {

    /** Taking what the plan uses from the network's storage. */
    GATHERING,

    /** Handing runs to executors and waiting for their outputs. */
    RUNNING,

    /** Stopped until the network has energy again; nothing is lost. */
    PAUSED,

    /** Done or cancelled: putting what it still holds back into the network. */
    RETURNING,

    /** Nothing left to do or hold. */
    DONE
}
