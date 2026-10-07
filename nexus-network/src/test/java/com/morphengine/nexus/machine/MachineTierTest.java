package com.morphengine.nexus.machine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineTierTest {

    @Test
    void tiersHaveOneThreeFiveAndSevenLines() {
        assertThat(MachineTier.BASIC.linePairs()).isEqualTo(1);
        assertThat(MachineTier.ADVANCED.linePairs()).isEqualTo(3);
        assertThat(MachineTier.SUPERIOR.linePairs()).isEqualTo(5);
        assertThat(MachineTier.QUANTUM.linePairs()).isEqualTo(7);
    }

    @Test
    void everyTierIsBiggerAndFasterThanTheOneBelow() {
        for (int rank = 2; rank <= 4; rank++) {
            final MachineTier below = MachineTier.ofRank(rank - 1);
            final MachineTier tier = MachineTier.ofRank(rank);

            assertThat(tier.bufferCapacity()).isGreaterThan(below.bufferCapacity());
            assertThat(tier.speedPercent()).isGreaterThan(below.speedPercent());
        }
    }

    @Test
    void nextWalksUpOneTierAtATime() {
        assertThat(MachineTier.BASIC.next()).contains(MachineTier.ADVANCED);
        assertThat(MachineTier.SUPERIOR.next()).contains(MachineTier.QUANTUM);
        assertThat(MachineTier.QUANTUM.next()).isEmpty();
    }

    @Test
    void aRankNoTierHasIsRefused() {
        assertThatThrownBy(() -> MachineTier.ofRank(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MachineTier.ofRank(5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTierWithANonPositiveValueIsRefused() {
        assertThatThrownBy(() -> new MachineTier(1, 0, 100, 10, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}
