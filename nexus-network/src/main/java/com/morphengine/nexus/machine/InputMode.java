package com.morphengine.nexus.machine;

/**
 * How resources put into a machine are spread over its input slots; the player switches it.
 */
public enum InputMode {

    /**
     * Each new resource takes a free slot of its own, and a resource already in a slot is added to that slot only.
     * Several resources work side by side, one to a line.
     */
    PER_RESOURCE,
    /**
     * The first resource takes all the slots and is spread over them evenly, so that every line works on it at once.
     * Another resource is refused until the slots are empty.
     */
    SPLIT
}
