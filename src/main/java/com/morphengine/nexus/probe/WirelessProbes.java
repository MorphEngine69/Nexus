package com.morphengine.nexus.probe;

import com.morphengine.nexus.block.entity.NetworkTransmitterBlockEntity;
import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import net.minecraft.network.chat.Component;

/**
 * What the Network Transmitter and the Nexus Link show of their reach.
 */
final class WirelessProbes {

    private WirelessProbes() {
    }

    static ProbeReport transmitter(final NetworkTransmitterBlockEntity transmitter) {
        return ProbeReport.builder()
                .text(DeviceProbes.named("gui.nexus.transmitter.", transmitter.status()))
                .build();
    }

    static ProbeReport link(final NexusLinkBlockEntity link) {
        return ProbeReport.builder()
                .text(Component.translatable("gui.nexus.link.range", link.range()))
                .build();
    }
}
