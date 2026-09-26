package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.transport.RedstoneMode;

import java.util.Objects;

/**
 * Whether a device may work, given its {@link RedstoneMode} and the redstone
 * signal it receives. In {@link RedstoneMode#PULSE} each signal that starts
 * lets exactly one operation through, however long the signal lasts; a pulse
 * not used yet is not saved. Server thread only.
 */
public final class RedstoneGate {

    private RedstoneMode mode;
    private boolean powered;
    private boolean pulsePending;

    public RedstoneGate(final RedstoneMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
    }

    public RedstoneMode mode() {
        return mode;
    }

    /**
     * Switches to {@code newMode}; a pulse received before is forgotten.
     */
    public void changeMode(final RedstoneMode newMode) {
        mode = Objects.requireNonNull(newMode, "mode must not be null");
        pulsePending = false;
    }

    /**
     * Takes the signal the device receives now. A signal that starts counts as
     * a pulse; the same signal reported again does not.
     */
    public void receive(final boolean signal) {
        if (signal && !powered) {
            pulsePending = true;
        }
        powered = signal;
    }

    /**
     * Takes the signal the device received when it was saved, without counting
     * it as a pulse.
     */
    public void restore(final boolean signal) {
        powered = signal;
        pulsePending = false;
    }

    public boolean isPowered() {
        return powered;
    }

    /**
     * @return whether the device may do an operation now
     */
    public boolean isOpen() {
        return switch (mode) {
            case IGNORED -> true;
            case HIGH_SIGNAL -> powered;
            case LOW_SIGNAL -> !powered;
            case PULSE -> pulsePending;
        };
    }

    /**
     * Tells the gate the device did an operation; in {@link RedstoneMode#PULSE}
     * that uses up the pending pulse.
     */
    public void operated() {
        pulsePending = false;
    }
}
