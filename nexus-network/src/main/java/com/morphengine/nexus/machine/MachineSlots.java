package com.morphengine.nexus.machine;

/**
 * Where the slots of a machine come from: the machine asks for the input slot and the output slot of a line by its
 * number and keeps what it gets, so that a game can supply slots that live in its containers.
 */
public interface MachineSlots {

    /**
     * @return the input slot of line {@code index}, the same slot every time it is asked for
     */
    MachineSlot input(int index);

    /**
     * @return the output slot of line {@code index}, the same slot every time it is asked for
     */
    MachineSlot output(int index);
}
