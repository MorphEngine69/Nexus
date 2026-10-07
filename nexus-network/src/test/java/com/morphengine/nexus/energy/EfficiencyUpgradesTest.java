package com.morphengine.nexus.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EfficiencyUpgradesTest {

    @Test
    void withoutUpgradesAMachinePaysTheFullPriceAndFuelGivesWhatItGives() {
        assertThat(EfficiencyUpgrades.costPercent(0)).isEqualTo(100);
        assertThat(EfficiencyUpgrades.yielded(40, 0)).isEqualTo(40);
    }

    @Test
    void eachUpgradeMakesTheWorkCheaperAndTheFuelRicher() {
        for (int count = 1; count <= EfficiencyUpgrades.MAX_UPGRADES; count++) {
            assertThat(EfficiencyUpgrades.costPercent(count)).isLessThan(EfficiencyUpgrades.costPercent(count - 1));
            assertThat(EfficiencyUpgrades.yielded(40, count)).isGreaterThan(EfficiencyUpgrades.yielded(40, count - 1));
        }
    }

    @Test
    void theMostUpgradesNeverMakeTheWorkFree() {
        assertThat(EfficiencyUpgrades.costPercent(EfficiencyUpgrades.MAX_UPGRADES)).isPositive();
    }

    @Test
    void aCountOutsideTheRangeIsRefused() {
        assertThatThrownBy(() -> EfficiencyUpgrades.costPercent(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EfficiencyUpgrades.yielded(40, EfficiencyUpgrades.MAX_UPGRADES + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
