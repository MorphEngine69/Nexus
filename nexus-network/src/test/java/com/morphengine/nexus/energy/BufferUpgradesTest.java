package com.morphengine.nexus.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BufferUpgradesTest {

    @Test
    void withoutUpgradesTheBufferKeepsItsSize() {
        assertThat(BufferUpgrades.scaled(1000, 0)).isEqualTo(1000);
    }

    @Test
    void anUpgradeAddsTheBaseSize() {
        assertThat(BufferUpgrades.scaled(1000, 1)).isEqualTo(2000);
        assertThat(BufferUpgrades.scaled(1000, BufferUpgrades.MAX_UPGRADES)).isEqualTo(2000);
    }

    @Test
    void aHugeBufferSaturatesInsteadOfOverflowing() {
        assertThat(BufferUpgrades.scaled(Long.MAX_VALUE, BufferUpgrades.MAX_UPGRADES)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void aCountOutsideTheRangeIsRefused() {
        assertThatThrownBy(() -> BufferUpgrades.scaled(1000, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BufferUpgrades.scaled(1000, BufferUpgrades.MAX_UPGRADES + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
