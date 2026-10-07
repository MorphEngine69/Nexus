package com.morphengine.nexus.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationPriceTest {

    @Test
    void efficiencyUpgradeTakesItsShareOffThePrice() {
        final long plain = OperationPrice.of(OperationKind.ASSEMBLER_RUN, 0, OperationUpgrades.NONE);

        assertThat(OperationPrice.of(OperationKind.ASSEMBLER_RUN, 0, new OperationUpgrades(0, 1)))
                .isEqualTo(plain * 70 / 100);
    }

    @Test
    void moreEfficiencyUpgradesThanAllowedCountAsTheMost() {
        assertThat(OperationPrice.of(OperationKind.TRANSFER, 0, new OperationUpgrades(0, 5)))
                .isEqualTo(OperationPrice.of(OperationKind.TRANSFER, 0, new OperationUpgrades(0, 1)));
    }

    @Test
    void aSmallNetworkPaysTheBaseCost() {
        for (OperationKind kind : OperationKind.values()) {
            assertThat(OperationPrice.of(kind, 0, 0)).isEqualTo(kind.baseCost());
            assertThat(OperationPrice.of(kind, 10, 0)).isEqualTo(kind.baseCost());
        }
    }

    @Test
    void theSizeFactorHitsTheSetPoints() {
        assertThat(OperationPrice.sizeFactor(50)).isEqualTo(1.5);
        assertThat(OperationPrice.sizeFactor(100)).isEqualTo(2.0);
        assertThat(OperationPrice.sizeFactor(250)).isEqualTo(3.5);
        assertThat(OperationPrice.sizeFactor(500)).isEqualTo(6.0);
    }

    @Test
    void betweenTheSetPointsTheSizeFactorRisesEvenly() {
        assertThat(OperationPrice.sizeFactor(30)).isEqualTo(1.25);
        assertThat(OperationPrice.sizeFactor(75)).isEqualTo(1.75);
    }

    @Test
    void beyondTheLargestSizeTheSizeFactorStops() {
        assertThat(OperationPrice.sizeFactor(5_000)).isEqualTo(6.0);
    }

    @Test
    void eachSpeedUpgradeDoublesThePrice() {
        for (int upgrades = 1; upgrades <= OperationPrice.MAX_SPEED_UPGRADES; upgrades++) {
            assertThat(OperationPrice.of(OperationKind.TRANSFER, 10, upgrades))
                    .isEqualTo(OperationPrice.of(OperationKind.TRANSFER, 10, upgrades - 1) * 2);
        }
    }

    @Test
    void aLargeNetworkAndFastDevicePayBoth() {
        assertThat(OperationPrice.of(OperationKind.ASSEMBLER_RUN, 500, 4)).isEqualTo(100 * 6 * 16);
    }

    @Test
    void anOperationIsNeverFree() {
        assertThat(OperationPrice.of(OperationKind.TERMINAL_TAKE, 0, 0)).isPositive();
    }

    @Test
    void impossibleCountsAreRefused() {
        assertThatThrownBy(() -> OperationPrice.sizeFactor(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OperationPrice.speedFactor(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OperationPrice.speedFactor(OperationPrice.MAX_SPEED_UPGRADES + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
