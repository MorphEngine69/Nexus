package com.morphengine.nexus.transport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SideConfigTest {

    private enum Face { TOP, BOTTOM, FRONT, BACK, LEFT, RIGHT }

    private enum Many { A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q }

    @Test
    void everySideStartsClosed() {
        final SideConfig<Face> config = SideConfig.closed(Face.class);

        for (Face face : Face.values()) {
            assertThat(config.mode(face)).isEqualTo(SideMode.CLOSED);
        }
    }

    @Test
    void withGivesANewConfigurationAndKeepsTheOldOne() {
        final SideConfig<Face> closed = SideConfig.closed(Face.class);

        final SideConfig<Face> opened = closed.with(Face.TOP, SideMode.INPUT);

        assertThat(opened.mode(Face.TOP)).isEqualTo(SideMode.INPUT);
        assertThat(opened.mode(Face.BOTTOM)).isEqualTo(SideMode.CLOSED);
        assertThat(closed.mode(Face.TOP)).isEqualTo(SideMode.CLOSED);
    }

    @Test
    void bitsRestoreEveryMode() {
        SideConfig<Face> config = SideConfig.closed(Face.class);
        config = config.with(Face.TOP, SideMode.INPUT).with(Face.BOTTOM, SideMode.OUTPUT)
                .with(Face.FRONT, SideMode.BOTH).with(Face.RIGHT, SideMode.INPUT);

        assertThat(SideConfig.fromBits(Face.class, config.toBits())).isEqualTo(config);
    }

    @Test
    void allClosedIsZeroBits() {
        assertThat(SideConfig.closed(Face.class).toBits()).isZero();
    }

    @Test
    void bitsOfSidesThatDoNotExistAreIgnored() {
        final int bits = SideConfig.closed(Face.class).with(Face.LEFT, SideMode.BOTH).toBits() | 0xF000;

        assertThat(SideConfig.fromBits(Face.class, bits).mode(Face.LEFT)).isEqualTo(SideMode.BOTH);
    }

    @Test
    void configurationsWithTheSameModesAreEqual() {
        final SideConfig<Face> one = SideConfig.closed(Face.class).with(Face.BACK, SideMode.OUTPUT);
        final SideConfig<Face> other = SideConfig.closed(Face.class).with(Face.BACK, SideMode.OUTPUT);

        assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
        assertThat(one).isNotEqualTo(SideConfig.closed(Face.class));
    }

    @Test
    void tooManySidesForOneIntAreRefused() {
        assertThatThrownBy(() -> SideConfig.closed(Many.class)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modesCycleThroughAllFourAndBack() {
        SideMode mode = SideMode.CLOSED;

        for (int step = 0; step < SideMode.values().length; step++) {
            mode = mode.next();
        }

        assertThat(mode).isEqualTo(SideMode.CLOSED);
        assertThat(SideMode.CLOSED.previous()).isEqualTo(SideMode.BOTH);
        assertThat(SideMode.INPUT.next().previous()).isEqualTo(SideMode.INPUT);
    }

    @Test
    void aDamagedModeClosesTheSide() {
        assertThat(SideMode.ofOrdinal(-1)).isEqualTo(SideMode.CLOSED);
        assertThat(SideMode.ofOrdinal(99)).isEqualTo(SideMode.CLOSED);
    }

    @Test
    void modesSayWhatTheyAllow() {
        assertThat(SideMode.INPUT.allowsInput()).isTrue();
        assertThat(SideMode.INPUT.allowsOutput()).isFalse();
        assertThat(SideMode.OUTPUT.allowsInput()).isFalse();
        assertThat(SideMode.OUTPUT.allowsOutput()).isTrue();
        assertThat(SideMode.BOTH.allowsInput()).isTrue();
        assertThat(SideMode.CLOSED.allowsOutput()).isFalse();
    }
}
