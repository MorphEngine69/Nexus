package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyPoolTest {

    @Test
    void emptyPoolHoldsNothing() {
        assertThat(EnergyPool.EMPTY.stored()).isZero();
        assertThat(EnergyPool.EMPTY.capacity()).isZero();
        assertThat(EnergyPool.EMPTY.insert(100, Action.EXECUTE)).isZero();
        assertThat(EnergyPool.EMPTY.extract(100, Action.EXECUTE)).isZero();
    }

    @Test
    void capacityAndStoredAreSummed() {
        final SimpleEnergyBuffer first = new SimpleEnergyBuffer(100, 100, 100);
        final SimpleEnergyBuffer second = new SimpleEnergyBuffer(300, 300, 300);
        first.insert(40, Action.EXECUTE);
        second.insert(60, Action.EXECUTE);

        final EnergyPool pool = new EnergyPool(List.of(first, second));

        assertThat(pool.capacity()).isEqualTo(400);
        assertThat(pool.stored()).isEqualTo(100);
    }

    @Test
    void insertSpillsIntoNextBufferWhenFirstIsFull() {
        final SimpleEnergyBuffer first = new SimpleEnergyBuffer(100, 1000, 1000);
        final SimpleEnergyBuffer second = new SimpleEnergyBuffer(100, 1000, 1000);
        final EnergyPool pool = new EnergyPool(List.of(first, second));

        final long accepted = pool.insert(150, Action.EXECUTE);

        assertThat(accepted).isEqualTo(150);
        assertThat(first.stored()).isEqualTo(100);
        assertThat(second.stored()).isEqualTo(50);
    }

    @Test
    void insertBeyondTotalCapacityIsPartial() {
        final EnergyPool pool = new EnergyPool(List.of(new SimpleEnergyBuffer(100, 1000, 1000)));

        assertThat(pool.insert(250, Action.EXECUTE)).isEqualTo(100);
    }

    @Test
    void extractDrainsAcrossBuffers() {
        final SimpleEnergyBuffer first = new SimpleEnergyBuffer(100, 1000, 1000);
        final SimpleEnergyBuffer second = new SimpleEnergyBuffer(100, 1000, 1000);
        first.insert(30, Action.EXECUTE);
        second.insert(50, Action.EXECUTE);
        final EnergyPool pool = new EnergyPool(List.of(first, second));

        final long removed = pool.extract(60, Action.EXECUTE);

        assertThat(removed).isEqualTo(60);
        assertThat(pool.stored()).isEqualTo(20);
    }

    @Test
    void simulationLeavesBuffersUntouched() {
        final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(100, 1000, 1000);
        final EnergyPool pool = new EnergyPool(List.of(buffer));

        assertThat(pool.insert(70, Action.SIMULATE)).isEqualTo(70);
        assertThat(buffer.stored()).isZero();
    }

    @Test
    void capacitySaturatesInsteadOfOverflowing() {
        final EnergyPool pool = new EnergyPool(List.of(
                new SimpleEnergyBuffer(Long.MAX_VALUE, 1, 1),
                new SimpleEnergyBuffer(Long.MAX_VALUE, 1, 1)));

        assertThat(pool.capacity()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void countersAreSummed() {
        final SimpleEnergyBuffer first = new SimpleEnergyBuffer(100, 1000, 1000);
        final SimpleEnergyBuffer second = new SimpleEnergyBuffer(100, 1000, 1000);
        first.insert(10, Action.EXECUTE);
        second.insert(20, Action.EXECUTE);
        second.extract(5, Action.EXECUTE);

        final EnergyPool pool = new EnergyPool(List.of(first, second));

        assertThat(pool.totalInserted()).isEqualTo(30);
        assertThat(pool.totalExtracted()).isEqualTo(5);
    }
}
