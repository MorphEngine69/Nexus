package com.morphengine.nexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Right click handling shared by every block whose block entity is a {@link MenuHost}.
 */
public final class MenuHosts {

    private MenuHosts() {
    }

    /**
     * Opens the menu of the block entity at {@code pos} on the server; the menu
     * reads the position from the buffer written here, then any data the host adds.
     */
    public static InteractionResult open(final Level level, final BlockPos pos, final Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuHost host && !host.ignoresClick()) {
            show(player, host, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Opens the menu of {@code host} for {@code player} right away, also when
     * the player already looks at it, such as after a change that needs a new
     * layout. Server side only.
     */
    public static void show(final Player player, final MenuHost host, final BlockPos pos) {
        player.openMenu(host, buffer -> {
            buffer.writeBlockPos(pos);
            host.writeMenuData(buffer);
        });
    }
}
