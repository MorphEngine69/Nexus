package com.morphengine.nexus.menu;

import com.morphengine.nexus.analysis.AnalyserLines;
import com.morphengine.nexus.analysis.Analysers;
import com.morphengine.nexus.analysis.CommonLines;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.entity.NetworkDeviceBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.level.DeviceEnergyRow;
import com.morphengine.nexus.level.EnergyContributor;
import com.morphengine.nexus.level.NetworkComponentTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Locale;
import java.util.Map;

/**
 * Works out what an Analyser shows about a block. Server thread only.
 */
public final class AnalyserViews {

    private AnalyserViews() {
    }

    /**
     * @param gameTime the current game time in ticks
     * @return the view of {@code target}; {@link AnalyserView#EMPTY} for a block that takes no part in a network's
     *         energy
     */
    public static AnalyserView of(final BlockEntity target, final long gameTime) {
        return switch (target) {
            case NexusBlockEntity nexus -> ofNexus(nexus, gameTime);
            case NetworkDeviceBlockEntity device -> ofDevice(device, gameTime);
            default -> AnalyserView.EMPTY;
        };
    }

    private static AnalyserView ofNexus(final NexusBlockEntity nexus, final long gameTime) {
        final NetworkStatistics statistics = nexus.statistics();
        final var portable = nexus.component(NetworkComponentTypes.ENERGY_ACCOUNT).portableTerminals().use(gameTime);
        final AnalyserLines lines = new AnalyserLines();
        lines.heading("network");
        lines.addTranslated("devices", "count", statistics.devices());
        for (DeviceRole role : DeviceRole.values()) {
            if (statistics.count(role) > 0) {
                lines.line(Component.translatable("gui.nexus.energy.role." + role.name().toLowerCase(Locale.ROOT)),
                        Component.literal(String.valueOf(statistics.count(role))));
            }
        }
        lines.heading("energy");
        lines.addTranslated("pool_in", "per_tick", String.valueOf(statistics.energyInput()));
        lines.addTranslated("pool_out", "per_tick", String.valueOf(statistics.energyOutput()));
        lines.addTranslated("portable_fees", "per_tick", AnalyserLines.perTick(portable.tolls()));
        return new AnalyserView(AnalyserView.Kind.NEXUS, Component.literal(nexus.network().name()), DeviceRole.OTHER,
                NetworkBadge.of(nexus), portable, statistics, lines.lines());
    }

    private static AnalyserView ofDevice(final NetworkDeviceBlockEntity device, final long gameTime) {
        final DeviceEnergyRow row = device.energyRow(gameTime);
        long stored = 0;
        long capacity = 0;
        if (device instanceof EnergyContributor holder) {
            stored = holder.energyBuffer().stored();
            capacity = holder.energyBuffer().capacity();
        }
        final AnalyserLines lines = new AnalyserLines();
        Analysers.describe(device, lines);
        CommonLines.addEnergy(lines, row.use());
        return new AnalyserView(AnalyserView.Kind.DEVICE, row.name(), row.role(), device.networkBadge(), row.use(),
                new NetworkStatistics(0, Map.of(), stored, capacity, 0, 0), lines.lines());
    }
}
