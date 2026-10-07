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
    void burningFasterGivesTheSameFuelTheSameEnergyInLessTime() {
        final FuelBurner slow = new FuelBurner(20);
        final FuelBurner fast = new FuelBurner(20);
        fast.setSpeed(4);
        slow.ignite(1600);
        fast.ignite(1600);
        final SimpleEnergyBuffer slowBuffer = new SimpleEnergyBuffer(100_000, 100_000, 100_000);
        final SimpleEnergyBuffer fastBuffer = new SimpleEnergyBuffer(100_000, 100_000, 100_000);

        int slowTicks = 0;
        while (slow.isBurning()) {
            slow.tick(slowBuffer);
            slowTicks++;
        }
        int fastTicks = 0;
        while (fast.isBurning()) {
            fast.tick(fastBuffer);
            fastTicks++;
        }

        assertThat(fastBuffer.stored()).isEqualTo(slowBuffer.stored()).isEqualTo(32_000);
        assertThat(fastTicks).isEqualTo(slowTicks / 4);
    }

    @Test
    void efficiencyUpgradesMakeTheSameFuelGiveMore() {
        final FuelBurner plain = new FuelBurner(40);
        plain.ignite(5);
        burner.setEfficiencyUpgrades(EfficiencyUpgrades.MAX_UPGRADES);
        burner.ignite(5);

        long plainTotal = 0;
        long boostedTotal = 0;
        for (int i = 0; i < 5; i++) {
            plainTotal += plain.tick(new SimpleEnergyBuffer(1000, 1000, 1000));
            boostedTotal += burner.tick(buffer);
        }

        assertThat(boostedTotal).isGreaterThan(plainTotal);
        assertThat(boostedTotal).isEqualTo(EfficiencyUpgrades.yielded(plainTotal, EfficiencyUpgrades.MAX_UPGRADES));
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
