package com.morphengine.nexus.api.network;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkTest {

    @Test
    void constructorTrimsName() {
        final Network network = new Network("  Noticed Nexus  ", NetworkColor.DEFAULT);

        assertThat(network.name()).isEqualTo("Noticed Nexus");
    }

    @Test
    void constructorRejectsBlankName() {
        assertThatThrownBy(() -> new Network("   ", NetworkColor.DEFAULT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsNameLongerThanLimit() {
        final String tooLong = "a".repeat(Network.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> new Network(tooLong, NetworkColor.DEFAULT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renameUpdatesName() {
        final Network network = new Network("Old name", NetworkColor.DEFAULT);

        network.rename("New name");

        assertThat(network.name()).isEqualTo("New name");
    }

    @Test
    void recolorUpdatesColor() {
        final Network network = new Network("Nexus", NetworkColor.DEFAULT);
        final NetworkColor red = new NetworkColor(0xFF0000);

        network.recolor(red);

        assertThat(network.color()).isEqualTo(red);
    }

    @Test
    void colorRejectsOutOfRangeValues() {
        assertThatThrownBy(() -> new NetworkColor(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NetworkColor(0x1000000)).isInstanceOf(IllegalArgumentException.class);
    }
}
