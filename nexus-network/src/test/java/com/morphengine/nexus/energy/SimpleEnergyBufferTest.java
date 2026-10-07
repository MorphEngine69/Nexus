package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleEnergyBufferTest {

    @Test
    void insertAcceptsUpToFreeSpace() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 1000, 1000);

        final long accepted = buffer.insert(150, Action.EXECUTE);

        assertThat(accepted).isEqualTo(100);
        assertThat(buffer.stored()).isEqualTo(100);
    }

    @Test
    void insertIsLimitedByRate() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(1000, 30, 1000);

        assertThat(buffer.insert(100, Action.EXECUTE)).isEqualTo(30);
    }

    @Test
    void simulatedInsertDoesNotChangeState() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);

        final long accepted = buffer.insert(40, Action.SIMULATE);

        assertThat(accepted).isEqualTo(40);
        assertThat(buffer.stored()).isZero();
        assertThat(buffer.totalInserted()).isZero();
    }

    @Test
    void extractFromEmptyBufferReturnsZero() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);

        assertThat(buffer.extract(50, Action.EXECUTE)).isZero();
    }

    @Test
    void extractIsLimitedByStoredAndRate() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 20);
        buffer.insert(50, Action.EXECUTE);

        assertThat(buffer.extract(100, Action.EXECUTE)).isEqualTo(20);
        assertThat(buffer.stored()).isEqualTo(30);
    }

    @Test
    void countersTrackExecutedTransfers() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);

        buffer.insert(60, Action.EXECUTE);
        buffer.extract(25, Action.EXECUTE);

        assertThat(buffer.totalInserted()).isEqualTo(60);
        assertThat(buffer.totalExtracted()).isEqualTo(25);
    }

    @Test
    void zeroAmountTransfersNothing() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);

        assertThat(buffer.insert(0, Action.EXECUTE)).isZero();
        assertThat(buffer.extract(0, Action.EXECUTE)).isZero();
    }

    @Test
    void hugeInsertDoesNotOverflow() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        buffer.insert(10, Action.EXECUTE);

        final long accepted = buffer.insert(Long.MAX_VALUE, Action.EXECUTE);

        assertThat(accepted).isEqualTo(Long.MAX_VALUE - 10);
        assertThat(buffer.stored()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void restoreReturnsToSnapshot() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);
        buffer.insert(10, Action.EXECUTE);
        final SimpleEnergyBuffer.Snapshot snapshot = buffer.snapshot();
        buffer.insert(50, Action.EXECUTE);

        buffer.restore(snapshot);

        assertThat(buffer.snapshot()).isEqualTo(snapshot);
    }

    @Test
    void restoreCutsStoredDownToCapacity() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);

        buffer.restore(SimpleEnergyBuffer.Snapshot.storing(500));

        assertThat(buffer.stored()).isEqualTo(100);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> new SimpleEnergyBuffer(0, 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimpleEnergyBuffer(1, -1, 1)).isInstanceOf(IllegalArgumentException.class);
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);
        assertThatThrownBy(() -> buffer.insert(-1, Action.EXECUTE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> buffer.extract(-1, Action.EXECUTE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resizeKeepsWhatTheBufferHolds() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);
        buffer.insert(80, Action.EXECUTE);

        buffer.resize(1000, 50, 50);

        assertThat(buffer.stored()).isEqualTo(80);
        assertThat(buffer.capacity()).isEqualTo(1000);
    }

    @Test
    void resizeAppliesTheNewRates() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(1000, 100, 100);

        buffer.resize(1000, 30, 20);

        assertThat(buffer.insert(500, Action.EXECUTE)).isEqualTo(30);
        assertThat(buffer.extract(500, Action.EXECUTE)).isEqualTo(20);
    }

    @Test
    void resizeToASmallerCapacityCutsWhatIsStored() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(1000, 1000, 1000);
        buffer.insert(900, Action.EXECUTE);

        buffer.resize(300, 1000, 1000);

        assertThat(buffer.stored()).isEqualTo(300);
    }

    @Test
    void resizeRejectsANonPositiveCapacity() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 100, 100);

        assertThatThrownBy(() -> buffer.resize(0, 10, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
