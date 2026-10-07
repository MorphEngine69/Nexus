package com.morphengine.nexus.api.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void constructorTrimsName() {
        final Network network = new Network(ID, "  Noticed Nexus  ", NetworkColor.DEFAULT);

        assertThat(network.name()).isEqualTo("Noticed Nexus");
    }

    @Test
    void constructorRejectsBlankName() {
        assertThatThrownBy(() -> new Network(ID, "   ", NetworkColor.DEFAULT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsNameLongerThanLimit() {
        final String tooLong = "a".repeat(Network.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> new Network(ID, tooLong, NetworkColor.DEFAULT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renameUpdatesName() {
        final Network network = new Network(ID, "Old name", NetworkColor.DEFAULT);

        network.rename("New name");

        assertThat(network.name()).isEqualTo("New name");
    }

    @Test
    void recolorUpdatesColor() {
        final Network network = new Network(ID, "Nexus", NetworkColor.DEFAULT);
        final NetworkColor red = new NetworkColor(0xFF0000);

        network.recolor(red);

        assertThat(network.color()).isEqualTo(red);
    }

    @Test
    void renamingAndRecoloringKeepTheId() {
        final Network network = new Network(ID, "Nexus", NetworkColor.DEFAULT);

        network.rename("Farm");
        network.recolor(new NetworkColor(0xFF0000));

        assertThat(network.id()).isEqualTo(ID);
    }

    @Test
    void constructorRejectsMissingId() {
        assertThatThrownBy(() -> new Network(null, "Nexus", NetworkColor.DEFAULT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void colorRejectsOutOfRangeValues() {
        assertThatThrownBy(() -> new NetworkColor(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NetworkColor(0x1000000)).isInstanceOf(IllegalArgumentException.class);
    }
}
