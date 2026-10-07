package com.morphengine.nexus.terminal;

/**
 * What a player at a terminal asks of a craft.
 */
public enum CraftRequest {

    /** Work out the plan and show it; nothing starts. */
    PREVIEW,

    /** Start the task, when the plan worked out now can start. */
    START,

    /**
     * Plan the most that can be crafted with nothing missing, up to the amount
     * asked, and show that plan; nothing starts.
     */
    CRAFT_LESS
}
