package com.morphengine.nexus.processing;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * What a machine looks like: dark without energy, ready, at work, or, for a machine whose parts cool slowly, cooling
 * down after work. Set by the server on the block state and turned into an animation by the renderer.
 */
public enum MachinePhase implements StringRepresentable {

    /** No energy in the buffer and none in the network. */
    OFF,
    /** Energy is there, nothing is being worked on. */
    STANDBY,
    /** A line has been working in the last moments. */
    ACTIVE,
    /** The work has just stopped and the machine shows its parts cooling. */
    COOLING;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return whether the accents of the model glow
     */
    public boolean isLit() {
        return this != OFF;
    }
}
