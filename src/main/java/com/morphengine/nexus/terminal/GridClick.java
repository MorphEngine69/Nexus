package com.morphengine.nexus.terminal;

/**
 * How the player clicked the grid of a terminal.
 */
public enum GridClick {

    /** Left button: a whole stack, or everything carried. */
    PRIMARY,

    /** Right button: half a stack, or one item carried. */
    SECONDARY,

    /** Shift with either button: straight into the player's inventory. */
    QUICK_MOVE
}
