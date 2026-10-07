package com.morphengine.nexus.probe;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.block.entity.CraftingMonitorBlockEntity;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.terminal.TerminalStatus;
import com.morphengine.nexus.transfer.TransferKind;
import com.morphengine.nexus.transfer.TransferResource;
import com.morphengine.nexus.transfer.TransferSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * What the devices that move things about show: the Puller and the Pusher with their kin, the Assembler, the Crafting
 * Monitor and the terminals.
 */
final class LogisticsProbes {

    private static final int LISTED_NAMES = 3;

    private LogisticsProbes() {
    }

    static ProbeReport transferDevice(final TransferDeviceBlockEntity device) {
        final TransferSettings settings = device.settings();
        final TransferKind kind = device.kind();
        final ProbeReport.Builder report = ProbeReport.builder();
        if (kind.hasWorldMode()) {
            report.text(Component.translatable("tooltip.nexus.probe.act." + kind.getSerializedName() + "."
                    + settings.worldMode().getSerializedName()));
        } else {
            report.text(DeviceProbes.labelled(Component.translatable("gui.nexus.transfer.resource"),
                    DeviceProbes.named("gui.nexus.transfer.resource.", settings.resource())));
        }
        if (settings.resource() != TransferResource.ENERGY) {
            report.text(filterSummary(kind, settings.filter()));
        }
        if (settings.redstone() != RedstoneMode.IGNORED) {
            report.text(DeviceProbes.labelled(Component.translatable("gui.nexus.transfer.redstone"),
                    DeviceProbes.named("gui.nexus.transfer.redstone.", settings.redstone())));
        }
        return report.build();
    }

    /**
     * What the filter lets a device work on, in a line: an empty list means everything for a device that takes things
     * out, and nothing for one that hands them out, unless it is a blacklist.
     */
    private static Component filterSummary(final TransferKind kind, final FilterSlots filter) {
        final boolean whitelist = filter.mode() == FilterMode.ALLOW;
        if (filter.entries().isEmpty()) {
            return Component.translatable(whitelist && kind.hasScheduling()
                    ? "tooltip.nexus.probe.filter.nothing" : "tooltip.nexus.probe.filter.anything");
        }
        return Component.translatable(whitelist ? "tooltip.nexus.probe.filter.only"
                : "tooltip.nexus.probe.filter.except", listed(filter));
    }

    private static Component listed(final FilterSlots filter) {
        final List<FilterSlots.Entry> entries = filter.inSlotOrder();
        final MutableComponent names = Component.empty();
        for (int i = 0; i < Math.min(entries.size(), LISTED_NAMES); i++) {
            final FilterSlots.Entry entry = entries.get(i);
            names.append(i == 0 ? Component.empty() : Component.literal(", "));
            names.append(entry.tag() != null ? Component.literal("#" + entry.tag()) : entry.resource().name());
        }
        if (entries.size() > LISTED_NAMES) {
            names.append(Component.literal(" +" + (entries.size() - LISTED_NAMES)));
        }
        return names;
    }

    static ProbeReport assembler(final AssemblerBlockEntity assembler) {
        return ProbeReport.builder()
                .text(DeviceProbes.labelled(Component.translatable("gui.nexus.assembler.blueprints"),
                        Component.literal(String.valueOf(assembler.blueprints().size()))))
                .text(Component.translatable("gui.nexus.assembler.tasks", assembler.taskStatuses().size()))
                .build();
    }

    static ProbeReport craftingMonitor(final CraftingMonitorBlockEntity monitor) {
        return ProbeReport.builder()
                .text(Component.translatable("tooltip.nexus.probe.jobs", monitor.tasks().size()))
                .build();
    }

    static ProbeReport terminal(final TerminalBlockEntity terminal) {
        final TerminalStatus status = terminal.status();
        if (status == TerminalStatus.ONLINE) {
            return ProbeReport.EMPTY;
        }
        return ProbeReport.builder()
                .text(DeviceProbes.named("gui.nexus.terminal.", status).copy().withStyle(ChatFormatting.RED))
                .build();
    }
}
