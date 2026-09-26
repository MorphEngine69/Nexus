package com.morphengine.nexus.api.transport;

/**
 * In which order a device that moves the resources listed in its filter tries
 * them. Each operation moves one resource: the first one tried that moves
 * anything.
 */
public enum SchedulingMode {

    /** Always from the first listed resource on. */
    IN_ORDER,

    /** From the resource after the one the previous operation moved, wrapping around. */
    ROUND_ROBIN,

    /** From a resource picked at random anew every operation, wrapping around. */
    RANDOM
}
