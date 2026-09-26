package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.transport.RedstoneMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedstoneGateTest {

    @Test
    void ignoredIsOpenWhateverTheSignal() {
        final RedstoneGate gate = new RedstoneGate(RedstoneMode.IGNORED);

        gate.receive(true);
        final boolean openWithSignal = gate.isOpen();
        gate.receive(false);

        assertThat(openWithSignal).isTrue();
        assertThat(gate.isOpen()).isTrue();
    }

    @Test
    void highSignalIsOpenOnlyWithASignal() {
        final RedstoneGate gate = new RedstoneGate(RedstoneMode.HIGH_SIGNAL);

        final boolean openWithout = gate.isOpen();
        gate.receive(true);

        assertThat(openWithout).isFalse();
        assertThat(gate.isOpen()).isTrue();
    }

    @Test
    void lowSignalIsOpenOnlyWithoutASignal() {
        final RedstoneGate gate = new RedstoneGate(RedstoneMode.LOW_SIGNAL);

        final boolean openWithout = gate.isOpen();
        gate.receive(true);

        assertThat(openWithout).isTrue();
        assertThat(gate.isOpen()).isFalse();
    }

    @Test
    void pulseLetsOneOperationThroughPerSignalStart() {
        final RedstoneGate gate = new RedstoneGate(RedstoneMode.PULSE);

        gate.receive(true);
        final boolean openAfterPulse = gate.isOpen();
        gate.operated();
        gate.receive(true);

        assertThat(openAfterPulse).isTrue();
        assertThat(gate.isOpen()).isFalse();
    }

    @Test
    void pulseOpensAgainForTheNextSignal() {
        final RedstoneGate gate = new RedstoneGate(RedstoneMode.PULSE);
        gate.receive(true);
        gate.operated();

        gate.receive(false);
        gate.receive(true);

        assertThat(gate.isOpen()).isTrue();
    }

    @Test
    void restoredSignalIsNoPulse() {
        final RedstoneGate gate = new RedstoneGate(RedstoneMode.PULSE);

        gate.restore(true);
        gate.receive(true);

        assertThat(gate.isOpen()).isFalse();
        assertThat(gate.isPowered()).isTrue();
    }

    @Test
    void changingTheModeForgetsAPendingPulse() {
        final RedstoneGate gate = new RedstoneGate(RedstoneMode.PULSE);
        gate.receive(true);

        gate.changeMode(RedstoneMode.HIGH_SIGNAL);
        gate.changeMode(RedstoneMode.PULSE);

        assertThat(gate.isOpen()).isFalse();
    }
}
