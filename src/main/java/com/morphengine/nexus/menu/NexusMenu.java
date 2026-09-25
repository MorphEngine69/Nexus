package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.networking.NexusStatisticsPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NexusMenu extends DeviceMenu<NexusBlockEntity> {

    /** On the server the figures last sent, on the client the figures last received. */
    private NetworkStatistics statistics = NetworkStatistics.EMPTY;

    public NexusMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.NEXUS.get(), containerId, inventory, pos, NexusBlockEntity.class);
    }

    public NetworkStatistics statistics() {
        return statistics;
    }

    public void acceptStatistics(final NetworkStatistics received) {
        statistics = received;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        final ServerPlayer viewer = viewer();
        final NexusBlockEntity nexus = blockEntity();
        if (viewer == null || nexus == null) {
            return;
        }
        final NetworkStatistics current = nexus.statistics();
        if (!current.equals(statistics)) {
            statistics = current;
            PacketDistributor.sendToPlayer(viewer, new NexusStatisticsPayload(containerId, current));
        }
    }
}
