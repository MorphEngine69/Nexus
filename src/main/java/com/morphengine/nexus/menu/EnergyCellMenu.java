package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.energy.EnergyRateMeter;
import com.morphengine.nexus.networking.EnergyCellViewPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Panel of one Energy Cell: its own charge and throughput, in the color of its
 * network. The server refreshes the view every {@value #REFRESH_INTERVAL_TICKS}
 * ticks and sends it only when it changed.
 */
public final class EnergyCellMenu extends DeviceMenu<EnergyCellBlockEntity> {

    private static final int REFRESH_INTERVAL_TICKS = 20;

    private final EnergyRateMeter meter = new EnergyRateMeter();
    private int ticksSinceOpen;
    /** On the server the view last sent, on the client the view last received. */
    private EnergyCellView view = EnergyCellView.EMPTY;

    public EnergyCellMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.ENERGY_CELL.get(), containerId, inventory, pos, EnergyCellBlockEntity.class);
    }

    public EnergyCellView view() {
        return view;
    }

    public void acceptView(final EnergyCellView received) {
        view = received;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        final ServerPlayer viewer = viewer();
        final EnergyCellBlockEntity cell = blockEntity();
        if (viewer == null || cell == null || ticksSinceOpen++ % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        final EnergyBuffer buffer = cell.energyBuffer();
        meter.sample(buffer, REFRESH_INTERVAL_TICKS);
        final EnergyCellView current = new EnergyCellView(buffer.stored(), buffer.capacity(),
                meter.inputPerTick(), meter.outputPerTick(), cell.networkBadge());
        if (!current.equals(view)) {
            view = current;
            PacketDistributor.sendToPlayer(viewer, new EnergyCellViewPayload(containerId, current));
        }
    }
}
