package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.entity.DeviceUpgrades;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.networking.NexusStatisticsPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Nexus panel: the network's figures, the upgrade slots and the player's
 * inventory.
 */
public final class NexusMenu extends DeviceMenu<NexusBlockEntity> {

    public static final int UPGRADES_LEFT = 208;
    public static final int UPGRADES_TOP = 39;
    public static final int INVENTORY_LEFT = 37;
    public static final int INVENTORY_TOP = 198;

    /** On the server the figures last sent, on the client the figures last received. */
    private NetworkStatistics statistics = NetworkStatistics.EMPTY;

    public NexusMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.NEXUS.get(), containerId, inventory, pos, NexusBlockEntity.class);
        final NexusBlockEntity nexus = blockEntity();
        UpgradeColumn.slots(viewer() != null && nexus != null ? nexus.upgrades() : null, UPGRADES_LEFT, UPGRADES_TOP)
                .forEach(this::addSlot);
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
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

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final boolean isUpgrade = UpgradeColumn.takes(slots.get(slotIndex).getItem());
        return shiftClick(slotIndex, DeviceUpgrades.SIZE, 0, isUpgrade ? DeviceUpgrades.SIZE : 0);
    }
}
