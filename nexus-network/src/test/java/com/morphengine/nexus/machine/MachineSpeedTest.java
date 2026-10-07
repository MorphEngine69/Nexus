package com.morphengine.nexus.machine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineSpeedTest {

    @Test
    void withoutUpgradesAMachineWorksAtTheSpeedOfItsTier() {
        assertThat(MachineSpeed.percent(MachineTier.ADVANCED, 0)).isEqualTo(MachineTier.ADVANCED.speedPercent());
    }

    @Test
    void eachSpeedUpgradeAddsSpeed() {
        for (int count = 1; count <= MachineSpeed.MAX_SPEED_UPGRADES; count++) {
            assertThat(MachineSpeed.percent(MachineTier.BASIC, count))
                    .isGreaterThan(MachineSpeed.percent(MachineTier.BASIC, count - 1));
        }
    }

    @Test
    void aFullySpedUpTierStaysBelowTheNextTier() {
        for (int rank = 1; rank <= 3; rank++) {
            final MachineTier tier = MachineTier.ofRank(rank);
            final MachineTier next = MachineTier.ofRank(rank + 1);

            assertThat(MachineSpeed.percent(tier, MachineSpeed.MAX_SPEED_UPGRADES)).isLessThan(next.speedPercent());
        }
    }

    @Test
    void theLastTierStillGainsFromUpgrades() {
        assertThat(MachineSpeed.percent(MachineTier.QUANTUM, MachineSpeed.MAX_SPEED_UPGRADES))
                .isGreaterThan(MachineTier.QUANTUM.speedPercent());
    }

    @Test
    void aCountOutsideTheRangeIsRefused() {
        assertThatThrownBy(() -> MachineSpeed.percent(MachineTier.BASIC, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MachineSpeed.percent(MachineTier.BASIC, MachineSpeed.MAX_SPEED_UPGRADES + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
