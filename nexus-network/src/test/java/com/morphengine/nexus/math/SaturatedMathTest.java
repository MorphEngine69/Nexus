package com.morphengine.nexus.math;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaturatedMathTest {

    @Test
    void addBelowLimitIsExact() {
        assertThat(SaturatedMath.add(40, 2)).isEqualTo(42);
    }

    @Test
    void addPastLimitStopsAtMaxValue() {
        assertThat(SaturatedMath.add(Long.MAX_VALUE - 1, 2)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void multiplyBelowLimitIsExact() {
        assertThat(SaturatedMath.multiply(1024, 8192)).isEqualTo(8_388_608);
    }

    @Test
    void multiplyPastLimitStopsAtMaxValue() {
        assertThat(SaturatedMath.multiply(Long.MAX_VALUE / 2, 3)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void multiplyByZeroIsZero() {
        assertThat(SaturatedMath.multiply(Long.MAX_VALUE, 0)).isZero();
    }
}
