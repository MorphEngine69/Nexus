package com.morphengine.nexus.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChargeMeterTest {

    private static final int SEGMENTS = 8;
    private static final long CAPACITY = 100_000;

    @Test
    void emptyBufferLightsNothing() {
        assertThat(ChargeMeter.segments(0, CAPACITY, SEGMENTS)).isZero();
    }

    @Test
    void fullBufferLightsEverySegment() {
        assertThat(ChargeMeter.segments(CAPACITY, CAPACITY, SEGMENTS)).isEqualTo(SEGMENTS);
    }

    @Test
    void anyChargeLightsAtLeastOneSegment() {
        assertThat(ChargeMeter.segments(1, CAPACITY, SEGMENTS)).isEqualTo(1);
    }

    @Test
    void nearlyFullBufferLeavesOneSegmentDark() {
        assertThat(ChargeMeter.segments(CAPACITY - 1, CAPACITY, SEGMENTS)).isEqualTo(SEGMENTS - 1);
    }

    @Test
    void sixtyPercentLightsFiveOfEight() {
        assertThat(ChargeMeter.segments(60_000, CAPACITY, SEGMENTS)).isEqualTo(5);
    }

    @Test
    void halfLightsHalf() {
        assertThat(ChargeMeter.segments(50_000, CAPACITY, SEGMENTS)).isEqualTo(4);
    }

    @Test
    void valuesOutsideTheRangeCountAsTheNearerEnd() {
        assertThat(ChargeMeter.segments(-5, CAPACITY, SEGMENTS)).isZero();
        assertThat(ChargeMeter.segments(CAPACITY * 2, CAPACITY, SEGMENTS)).isEqualTo(SEGMENTS);
    }

    @Test
    void hugeCapacityDoesNotOverflow() {
        assertThat(ChargeMeter.segments(Long.MAX_VALUE / 2, Long.MAX_VALUE, SEGMENTS)).isEqualTo(4);
    }

    @Test
    void nonPositiveArgumentsAreRejected() {
        assertThatThrownBy(() -> ChargeMeter.segments(1, 0, SEGMENTS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChargeMeter.segments(1, CAPACITY, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
