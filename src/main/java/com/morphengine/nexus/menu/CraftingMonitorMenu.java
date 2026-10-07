package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.entity.CraftingMonitorBlockEntity;
import com.morphengine.nexus.networking.CraftingMonitorPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Crafting Monitor panel: the crafting tasks of the network, sent to the
 * client every {@value #SYNC_INTERVAL_TICKS} ticks while they change, and a
 * way to cancel them.
 */
public final class CraftingMonitorMenu extends DeviceMenu<CraftingMonitorBlockEntity> implements NetworkBadgeView {

    private static final int SYNC_INTERVAL_TICKS = 10;

    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    private List<TaskStatus> tasks = List.of();
    private @Nullable List<TaskStatus> sent;
    private @Nullable NetworkBadge badge;
    private int ticks;

    public CraftingMonitorMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.CRAFTING_MONITOR.get(), containerId, inventory, pos, CraftingMonitorBlockEntity.class);
    }

    /**
     * @return the network's tasks as last received; empty on the server
     */
    public List<TaskStatus> tasks() {
        return tasks;
    }

    public void acceptTasks(final List<TaskStatus> received) {
        tasks = List.copyOf(received);
    }

    /**
     * Cancels a task of the network, for a player who may order crafting there.
     * Server side only.
     */
    public void cancel(final Player player, final UUID task) {
        final CraftingMonitorBlockEntity monitor = blockEntity();
        if (monitor != null && permits(player, Permission.AUTOCRAFTING)) {
            monitor.cancel(task);
        }
    }

    @Override
    public @Nullable NetworkBadge badge() {
        return badge;
    }

    @Override
    public void acceptBadge(final @Nullable NetworkBadge received) {
        badge = received;
    }

    @Override
    public void broadcastChanges() {
        final CraftingMonitorBlockEntity monitor = blockEntity();
        final ServerPlayer viewer = viewer();
        if (monitor != null && viewer != null) {
            badgeSync.tick(viewer, containerId, monitor.networkBadge());
            if (ticks++ % SYNC_INTERVAL_TICKS == 0) {
                sendTasks(viewer, monitor.tasks());
            }
        }
        super.broadcastChanges();
    }

    private void sendTasks(final ServerPlayer viewer, final List<TaskStatus> current) {
        if (!current.equals(sent)) {
            sent = current;
            PacketDistributor.sendToPlayer(viewer, new CraftingMonitorPayload(containerId, current));
        }
    }
}
