package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.network.DeviceEnergyUse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceEnergyMeterTest {

    private static final int SECOND = DeviceEnergyMeter.TICKS_PER_SECOND;

    private final DeviceEnergyMeter meter = new DeviceEnergyMeter();

    @Test
    void firstReadingOnlySetsBaseline() {
        meter.recordDrawn(500);

        assertThat(meter.use(100)).isEqualTo(DeviceEnergyUse.NONE);
    }

    @Test
    void reportsFigureOfTheSecondPerSecond() {
        meter.use(0);
        meter.recordDrawn(100);
        meter.recordSupplied(40);
        meter.recordToll(7);

        assertThat(meter.use(SECOND)).isEqualTo(new DeviceEnergyUse(100, 40, 7));
    }

    @Test
    void keepsFiguresWhileASecondIsNotOver() {
        meter.use(0);
        meter.recordDrawn(100);
        meter.use(SECOND);
        meter.recordDrawn(900);

        assertThat(meter.use(SECOND + SECOND / 2).drawn()).isEqualTo(100);
    }

    @Test
    void averagesOverTheSecondsMeasured() {
        meter.use(0);
        meter.recordToll(300);
        meter.use(SECOND);
        meter.use(2 * SECOND);
        meter.use(3 * SECOND);

        assertThat(meter.use(3 * SECOND).tolls()).isEqualTo(100);
    }

    @Test
    void burstShowsTheSameFigureWhenReadAtAnyMoment() {
        meter.use(0);
        for (int second = 1; second <= 10; second++) {
            if (second == 4) {
                meter.recordToll(162);
            }
            meter.use(second * SECOND);
        }

        assertThat(meter.use(10 * SECOND).tolls()).isEqualTo(16);
        assertThat(meter.use(10 * SECOND + 5).tolls()).isEqualTo(16);
    }

    @Test
    void burstLeavesTheWindowAfterTenSeconds() {
        meter.use(0);
        meter.recordToll(500);
        for (int second = 1; second <= 11; second++) {
            meter.use(second * SECOND);
        }

        assertThat(meter.use(11 * SECOND).tolls()).isZero();
    }

    @Test
    void gapSpreadsWhatWasCountedInIt() {
        meter.use(0);
        meter.recordDrawn(400);

        assertThat(meter.use(4 * SECOND).drawn()).isEqualTo(100);
    }

    @Test
    void timeGoingBackwardsStartsOver() {
        meter.use(1_000);
        meter.recordDrawn(100);

        assertThat(meter.use(5)).isEqualTo(DeviceEnergyUse.NONE);
        meter.recordDrawn(60);
        assertThat(meter.use(5 + SECOND).drawn()).isEqualTo(60);
    }

    @Test
    void nonPositiveMovementIsRejected() {
        assertThatThrownBy(() -> meter.recordToll(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
