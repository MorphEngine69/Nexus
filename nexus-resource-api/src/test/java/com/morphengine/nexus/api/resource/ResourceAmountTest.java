package com.morphengine.nexus.api.resource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceAmountTest {

    private static final ResourceKey RESOURCE = () -> new ResourceType() {
    };

    @Test
    void zeroAmountIsRejected() {
        assertThatThrownBy(() -> new ResourceAmount(RESOURCE, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeAmountIsRejected() {
        assertThatThrownBy(() -> new ResourceAmount(RESOURCE, -5)).isInstanceOf(IllegalArgumentException.class);
    }
}
