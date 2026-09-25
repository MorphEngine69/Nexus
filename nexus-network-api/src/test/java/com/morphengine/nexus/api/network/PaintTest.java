package com.morphengine.nexus.api.network;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaintTest {

    private static final Paint RED = Paint.dye(14);
    private static final Paint BLUE = Paint.dye(11);

    @Test
    void sameDyeConnects() {
        assertThat(RED.connectsTo(Paint.dye(14))).isTrue();
    }

    @Test
    void differentDyesDoNotConnect() {
        assertThat(RED.connectsTo(BLUE)).isFalse();
        assertThat(BLUE.connectsTo(RED)).isFalse();
    }

    @Test
    void unpaintedBridgesAnyDye() {
        assertThat(Paint.UNPAINTED.connectsTo(RED)).isTrue();
        assertThat(RED.connectsTo(Paint.UNPAINTED)).isTrue();
    }

    @Test
    void unpaintedConnectsToUnpainted() {
        assertThat(Paint.UNPAINTED.connectsTo(Paint.UNPAINTED)).isTrue();
    }

    @Test
    void dyeRejectsNegativeId() {
        assertThatThrownBy(() -> Paint.dye(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
