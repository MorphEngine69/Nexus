package com.morphengine.nexus.menu;

import com.morphengine.nexus.analysis.AnalyserLine;
import com.morphengine.nexus.api.network.DeviceEnergyUse;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkStatistics;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What the panel of an Analyser shows about the block it was used on. Energy in FE, rates in FE per second.
 *
 * @param kind    what was analysed
 * @param name    what the block is called
 * @param role    what a device does; other for a Nexus
 * @param network the network the block is in; {@code null} when no Nexus is connected
 * @param use     what a device draws, supplies and pays in fees; for a Nexus, what its portable terminals pay
 * @param energy  for a Nexus, the figures of its network; for a device, only what its buffer holds, if it has one
 * @param lines   what the block tells about itself, in the order the panel shows it; copied
 */
public record AnalyserView(
        Kind kind, Component name, DeviceRole role, @Nullable NetworkBadge network, DeviceEnergyUse use,
        NetworkStatistics energy, List<AnalyserLine> lines) {

    public static final AnalyserView EMPTY = new AnalyserView(Kind.NOTHING, Component.empty(), DeviceRole.OTHER,
            null, DeviceEnergyUse.NONE, NetworkStatistics.EMPTY, List.of());

    public AnalyserView {
        lines = List.copyOf(lines);
    }

    /**
     * What an Analyser was used on.
     */
    public enum Kind {
        NOTHING, DEVICE, NEXUS
    }
}
