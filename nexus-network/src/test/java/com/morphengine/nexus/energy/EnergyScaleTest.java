package com.morphengine.nexus.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnergyScaleTest {

    @Test
    void multiplierBecomesAShareInPercent() {
        assertThat(EnergyScale.percentOf(1.0)).isEqualTo(100);
        assertThat(EnergyScale.percentOf(1.5)).isEqualTo(150);
        assertThat(EnergyScale.percentOf(0.25)).isEqualTo(25);
    }

    @Test
    void multiplierIsKeptWithinWhatIsAllowed() {
        assertThat(EnergyScale.percentOf(0.0)).isEqualTo(EnergyScale.MIN_PERCENT);
        assertThat(EnergyScale.percentOf(1_000_000.0)).isEqualTo(EnergyScale.MAX_PERCENT);
        assertThat(EnergyScale.percentOf(Double.NaN)).isEqualTo(EnergyScale.NEUTRAL_PERCENT);
    }

    @Test
    void neutralShareChangesNothing() {
        assertThat(EnergyScale.of(12_345, 100)).isEqualTo(12_345);
        assertThat(EnergyScale.priceOf(37, 100)).isEqualTo(37);
    }

    @Test
    void shareOfAnAmountIsRoundedDown() {
        assertThat(EnergyScale.of(7, 150)).isEqualTo(10);
        assertThat(EnergyScale.of(1, 50)).isZero();
    }

    @Test
    void largeAmountDoesNotOverflow() {
        assertThat(EnergyScale.of(Long.MAX_VALUE, EnergyScale.MAX_PERCENT)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void priceIsRoundedAndNeverBelowOne() {
        assertThat(EnergyScale.priceOf(10, 25)).isEqualTo(3);
        assertThat(EnergyScale.priceOf(1, 1)).isEqualTo(1);
    }

    @Test
    void shareOutOfRangeIsRejected() {
        assertThatThrownBy(() -> EnergyScale.of(10, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnergyScale.priceOf(10, EnergyScale.MAX_PERCENT + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeAmountIsRejected() {
        assertThatThrownBy(() -> EnergyScale.of(-1, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}
