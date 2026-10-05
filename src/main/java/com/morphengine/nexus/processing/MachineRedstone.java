package com.morphengine.nexus.processing;

import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.machine.Machine;
import com.morphengine.nexus.machine.MachineLine;
import com.morphengine.nexus.transport.RedstoneGate;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Objects;

/**
 * How a machine reacts to a redstone signal. With a signal that lets it work or not it simply works or waits; in
 * {@link RedstoneMode#PULSE} each signal that starts lets the machine run until its lines have finished the jobs they
 * took up, and a pulse that finds nothing to do is spent all the same. Server thread only.
 */
public final class MachineRedstone {

    private static final String TAG_MODE = "redstone_mode";
    private static final String TAG_SIGNAL = "redstone_signal";

    private final RedstoneGate gate = new RedstoneGate(RedstoneMode.IGNORED);
    private final Runnable changed;
    private boolean pulseRunning;
    private boolean signalKnown;

    /**
     * @param changed called when the mode or the signal changes, so that the owner saves them
     */
    public MachineRedstone(final Runnable changed) {
        this.changed = Objects.requireNonNull(changed, "changed must not be null");
    }

    public RedstoneMode mode() {
        return gate.mode();
    }

    public void changeMode(final RedstoneMode mode) {
        if (gate.mode() != mode) {
            gate.changeMode(mode);
            pulseRunning = false;
            changed.run();
        }
    }

    public boolean isSignalKnown() {
        return signalKnown;
    }

    /**
     * Takes the signal the machine receives now.
     */
    public void receive(final boolean signal) {
        final boolean differs = gate.isPowered() != signal;
        gate.receive(signal);
        signalKnown = true;
        if (differs) {
            changed.run();
        }
    }

    /**
     * Takes the signal found when nothing is known of it yet, without counting it as a pulse.
     */
    public void restore(final boolean signal) {
        gate.restore(signal);
        signalKnown = true;
    }

    /**
     * @return whether the machine may work in this tick; in {@link RedstoneMode#PULSE} a signal that has started and
     *         is not taken up yet is taken up by this call
     */
    public boolean permitsWork() {
        if (gate.mode() != RedstoneMode.PULSE) {
            return gate.isOpen();
        }
        if (!pulseRunning && gate.isOpen()) {
            pulseRunning = true;
            gate.operated();
        }
        return pulseRunning;
    }

    /**
     * Tells that the machine worked in this tick; a pulse ends once no line of the machine is running.
     */
    public void worked(final Machine machine) {
        if (!pulseRunning) {
            return;
        }
        for (int index = 0; index < machine.lineCount(); index++) {
            final MachineLine line = machine.line(index);
            if (line.isRunning()) {
                return;
            }
        }
        pulseRunning = false;
    }

    public void save(final ValueOutput output) {
        output.putInt(TAG_MODE, gate.mode().ordinal());
        output.putBoolean(TAG_SIGNAL, gate.isPowered());
    }

    public void load(final ValueInput input) {
        final RedstoneMode[] modes = RedstoneMode.values();
        gate.changeMode(modes[Math.clamp(input.getIntOr(TAG_MODE, 0), 0, modes.length - 1)]);
        gate.restore(input.getBooleanOr(TAG_SIGNAL, false));
        pulseRunning = false;
        signalKnown = false;
    }
}
