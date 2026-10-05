package com.morphengine.nexus.machine;

/**
 * What a line, or a whole machine, is doing in a tick. The order is the order of importance: of several activities
 * the machine shows the last one in this list.
 */
public enum MachineActivity {

    /** Nothing to work on. */
    IDLE,
    /** The work is done but the output slot has no room for the result, or has none for a new job. */
    OUTPUT_BLOCKED,
    /** There is work but the buffer holds too little FE for this tick. */
    WAITING_FOR_ENERGY,
    /** The work moved on this tick. */
    WORKING;

    /**
     * @return the more important of the two activities
     */
    public MachineActivity or(final MachineActivity other) {
        return other.ordinal() > ordinal() ? other : this;
    }
}
