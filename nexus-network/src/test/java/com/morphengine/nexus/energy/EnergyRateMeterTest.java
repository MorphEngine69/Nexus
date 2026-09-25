package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnergyRateMeterTest {

    private final SimpleEnergyBuffer buffer = new SimpleEnergyBuffer(1_000_000, 1_000_000, 1_000_000);
    private final EnergyRateMeter meter = new EnergyRateMeter();

    @Test
    void firstSampleOnlySetsBaseline() {
        buffer.insert(500, Action.EXECUTE);

        meter.sample(buffer, 20);

        assertThat(meter.inputPerTick()).isZero();
        assertThat(meter.outputPerTick()).isZero();
    }

    @Test
    void ratesAreAveragedOverElapsedTicks() {
        meter.sample(buffer, 20);
        buffer.insert(2000, Action.EXECUTE);
        buffer.extract(400, Action.EXECUTE);

        meter.sample(buffer, 20);

        assertThat(meter.inputPerTick()).isEqualTo(100);
        assertThat(meter.outputPerTick()).isEqualTo(20);
    }

    @Test
    void idlePeriodReportsZero() {
        meter.sample(buffer, 20);
        buffer.insert(2000, Action.EXECUTE);
        meter.sample(buffer, 20);

        meter.sample(buffer, 20);

        assertThat(meter.inputPerTick()).isZero();
    }

    @Test
    void rebaseDropsThePeriodAfterBufferReplacement() {
        meter.sample(buffer, 20);
        final SimpleEnergyBuffer replacement = new SimpleEnergyBuffer(100, 100, 100);
        replacement.insert(100, Action.EXECUTE);
        meter.rebase();

        meter.sample(replacement, 20);

        assertThat(meter.inputPerTick()).isZero();
    }

    @Test
    void countersGoingBackwardsReportZero() {
        buffer.insert(1000, Action.EXECUTE);
        meter.sample(buffer, 20);

        meter.sample(new SimpleEnergyBuffer(100, 100, 100), 20);

        assertThat(meter.inputPerTick()).isZero();
    }

    @Test
    void rejectsNonPositiveElapsedTicks() {
        assertThatThrownBy(() -> meter.sample(buffer, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
