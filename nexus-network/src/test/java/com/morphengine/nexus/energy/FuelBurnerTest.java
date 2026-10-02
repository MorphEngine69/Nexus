package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FuelBurnerTest {

    private final FuelBurner burner = new FuelBurner(40);
    private final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(1000, 1000, 1000);

    @Test
    void idleBurnerProducesNothing() {
        assertThat(burner.tick(buffer)).isZero();
        assertThat(buffer.stored()).isZero();
    }

    @Test
    void burningProducesRateEveryTickUntilFuelRunsOut() {
        burner.ignite(3);

        long total = 0;
        for (int i = 0; i < 5; i++) {
            total += burner.tick(buffer);
        }

        assertThat(total).isEqualTo(120);
        assertThat(burner.isBurning()).isFalse();
    }

    @Test
    void fullBufferPausesBurningWithoutWastingFuel() {
        buffer.insert(980, Action.EXECUTE);
        burner.ignite(10);

        final long produced = burner.tick(buffer);

        assertThat(produced).isZero();
        assertThat(burner.burnTicksLeft()).isEqualTo(10);
    }

    @Test
    void hasRoomOnlyForAFullTickOfOutput() {
        buffer.insert(961, Action.EXECUTE);

        assertThat(burner.hasRoomIn(buffer)).isFalse();
    }

    @Test
    void igniteWhileBurningIsRejected() {
        burner.ignite(5);

        assertThatThrownBy(() -> burner.ignite(5)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> new FuelBurner(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> burner.ignite(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void higherSpeedBurnsSeveralTicksOfFuelEachTick() {
        burner.setSpeed(3);
        burner.ignite(10);

        final long produced = burner.tick(buffer);

        assertThat(produced).isEqualTo(120);
        assertThat(burner.burnTicksLeft()).isEqualTo(7);
    }

    @Test
    void theSameFuelGivesTheSameEnergyAtAnySpeed() {
        burner.setSpeed(4);
        burner.ignite(10);

        long total = 0;
        for (int i = 0; i < 5; i++) {
            total += burner.tick(buffer);
        }

        assertThat(total).isEqualTo(400);
        assertThat(burner.isBurning()).isFalse();
    }

    @Test
    void roomIsCheckedForAFullTickAtTheCurrentSpeed() {
        burner.setSpeed(2);
        buffer.insert(930, Action.EXECUTE);

        assertThat(burner.hasRoomIn(buffer)).isFalse();
    }

    @Test
    void rejectsASpeedBelowOne() {
        assertThatThrownBy(() -> burner.setSpeed(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoreClampsTicksLeftToTotal() {
        burner.restore(50, 20);

        assertThat(burner.burnTicksLeft()).isEqualTo(20);
        assertThat(burner.burnTicksTotal()).isEqualTo(20);
    }
}
