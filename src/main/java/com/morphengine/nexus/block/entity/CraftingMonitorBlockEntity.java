package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.block.CraftingMonitorBlock;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.menu.CraftingMonitorMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * A Crafting Monitor: a window onto the crafting tasks of its network. Once a
 * second it shows on its block whether the network has energy and tasks.
 */
public final class CraftingMonitorBlockEntity extends NetworkDeviceBlockEntity implements Renamable {

    private static final int STATE_CHECK_INTERVAL_TICKS = 20;

    public CraftingMonitorBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.CRAFTING_MONITOR.get(), pos, state);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final CraftingMonitorBlockEntity monitor) {
        if (level.getGameTime() % STATE_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        final BlockState shown = state.setValue(CraftingMonitorBlock.POWERED, monitor.isNetworkPowered())
                .setValue(CraftingMonitorBlock.ACTIVE, !monitor.tasks().isEmpty());
        if (shown != state) {
            level.setBlock(pos, shown, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * @return every crafting task of the network; empty without a network. Server side only.
     */
    public List<TaskStatus> tasks() {
        final NetworkController controller = controller();
        return controller != null ? controller.component(NetworkComponentTypes.AUTOCRAFTING).statuses() : List.of();
    }

    /**
     * Cancels the network's task of that id, if it still has one. Server side only.
     */
    public void cancel(final UUID task) {
        final NetworkController controller = controller();
        if (controller != null) {
            controller.component(NetworkComponentTypes.AUTOCRAFTING).cancel(task);
        }
    }

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new CraftingMonitorMenu(containerId, inventory, worldPosition);
    }
}
