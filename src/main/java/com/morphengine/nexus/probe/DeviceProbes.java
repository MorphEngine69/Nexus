package com.morphengine.nexus.probe;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.entity.NetworkDeviceBlockEntity;
import com.morphengine.nexus.block.entity.StandaloneDevice;
import com.morphengine.nexus.menu.NetworkBadge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import java.util.Locale;

/**
 * The lines every device of a network shows: which network it is in, whether that network has energy, who it works for
 * and what stops it. A device that has no network says so, unless it works without one. Also the way a label and its
 * value are written, which the other descriptions share.
 */
final class DeviceProbes {

    private DeviceProbes() {
    }

    static ProbeReport of(final NetworkDeviceBlockEntity device) {
        final ProbeReport.Builder report = ProbeReport.builder();
        final NetworkBadge badge = device.networkBadge();
        if (badge == null) {
            report.text(device instanceof StandaloneDevice
                    ? Component.translatable("gui.nexus.standalone").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("gui.nexus.terminal.no_network").withStyle(ChatFormatting.RED));
            return report.build();
        }
        report.text(network(badge));
        if (!device.isNetworkPowered()) {
            report.text(Component.translatable("gui.nexus.terminal.no_energy").withStyle(ChatFormatting.RED));
        }
        final Permission missing = device.missingPermission();
        if (missing != null) {
            report.text(Component.translatable("gui.nexus.access.halted", NetworkAccess.nameOf(missing))
                    .withStyle(ChatFormatting.RED));
        }
        final NameAndId owner = device.ownerProfile();
        if (owner != null) {
            report.text(Component.translatable("gui.nexus.access.owner", owner.name())
                    .withStyle(ChatFormatting.GRAY));
        }
        return report.build();
    }

    /**
     * @return the network as the panels show it: its name in its color
     */
    static Component network(final NetworkBadge badge) {
        return Component.translatable("gui.nexus.network",
                Component.literal(badge.name()).withColor(badge.color().rgb()));
    }

    static Component labelled(final Component label, final Component value) {
        return Component.translatable("tooltip.nexus.probe.labelled", label, value);
    }

    static Component named(final String keyPrefix, final Enum<?> value) {
        return Component.translatable(keyPrefix + value.name().toLowerCase(Locale.ROOT));
    }
}
