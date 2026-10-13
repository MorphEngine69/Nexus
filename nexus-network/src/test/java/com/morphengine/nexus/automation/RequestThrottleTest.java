package com.morphengine.nexus.automation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestThrottleTest {

    private final RequestThrottle throttle = new RequestThrottle(3);

    @Test
    void theFirstRequestIsAlwaysLetThrough() {
        assertThat(throttle.tryAdmit(0)).isTrue();
    }

    @Test
    void theFirstRequestIsLetThroughAtAnyTime() {
        assertThat(new RequestThrottle(3).tryAdmit(Long.MAX_VALUE)).isTrue();
        assertThat(new RequestThrottle(3).tryAdmit(1_000_000)).isTrue();
    }

    @Test
    void aRequestInsideTheGapIsTurnedAway() {
        throttle.tryAdmit(100);

        assertThat(throttle.tryAdmit(101)).isFalse();
        assertThat(throttle.tryAdmit(102)).isFalse();
    }

    @Test
    void aRequestAfterTheGapIsLetThrough() {
        throttle.tryAdmit(100);

        assertThat(throttle.tryAdmit(103)).isTrue();
    }

    @Test
    void aRequestTurnedAwayDoesNotMoveTheGap() {
        throttle.tryAdmit(100);
        throttle.tryAdmit(102);

        assertThat(throttle.tryAdmit(103)).isTrue();
    }

    @Test
    void aGapOfZeroLetsEverythingThrough() {
        final RequestThrottle open = new RequestThrottle(0);

        assertThat(open.tryAdmit(5)).isTrue();
        assertThat(open.tryAdmit(5)).isTrue();
    }

    @Test
    void rejectsANegativeGap() {
        assertThatThrownBy(() -> new RequestThrottle(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
