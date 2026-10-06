package com.morphengine.nexus.machine;

import com.morphengine.nexus.transport.SideMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MachineSidesTest {

    @Test
    void everySideOfAnOpenDeviceTakesAndGives() {
        for (MachineSide side : MachineSide.values()) {
            assertThat(MachineSides.allOpen().mode(side)).isEqualTo(SideMode.BOTH);
        }
    }

    @Test
    void theDefaultsOfAMachineKeepSomeSidesToOneUse() {
        assertThat(MachineSides.defaults().mode(MachineSide.TOP)).isEqualTo(SideMode.INPUT);
        assertThat(MachineSides.defaults().mode(MachineSide.BOTTOM)).isEqualTo(SideMode.OUTPUT);
    }
}
