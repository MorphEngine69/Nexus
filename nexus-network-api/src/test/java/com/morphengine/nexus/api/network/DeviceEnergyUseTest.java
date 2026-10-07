package com.morphengine.nexus.api.network;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceEnergyUseTest {

    @Test
    void spentAddsWorkAndOperations() {
        assertThat(new DeviceEnergyUse(30, 5, 12).spent()).isEqualTo(42);
    }

    @Test
    void noneSpendsAndSuppliesNothing() {
        assertThat(DeviceEnergyUse.NONE.spent()).isZero();
        assertThat(DeviceEnergyUse.NONE.supplied()).isZero();
    }

    @Test
    void negativeFigureIsRejected() {
        assertThatThrownBy(() -> new DeviceEnergyUse(0, -1, 0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supplied");
    }
}
