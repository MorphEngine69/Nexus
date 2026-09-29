package com.morphengine.nexus.transport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferRateTest {

    @Test
    void withoutUpgradesItWorksAtBaseRate() {
        assertThat(TransferRate.of(0, 0)).isEqualTo(new TransferRate(10, 1));
    }

    @Test
    void everySpeedUpgradeShortensTheInterval() {
        assertThat(TransferRate.of(1, 0).intervalTicks()).isEqualTo(8);
        assertThat(TransferRate.of(4, 0).intervalTicks()).isEqualTo(2);
    }

    @Test
    void intervalNeverDropsBelowTheMinimum() {
        assertThat(TransferRate.of(20, 0).intervalTicks()).isEqualTo(TransferRate.MIN_INTERVAL_TICKS);
    }

    @Test
    void stackUpgradeMultipliesWhatAnOperationMoves() {
        assertThat(TransferRate.of(0, 1).multiplier()).isEqualTo(64);
    }

    @Test
    void moreStackUpgradesCountAsOne() {
        assertThat(TransferRate.of(0, 3).multiplier()).isEqualTo(64);
    }

    @Test
    void speedAndStackCombine() {
        assertThat(TransferRate.of(2, 1)).isEqualTo(new TransferRate(6, 64));
    }

    @Test
    void negativeCountIsRejected() {
        assertThatThrownBy(() -> TransferRate.of(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
