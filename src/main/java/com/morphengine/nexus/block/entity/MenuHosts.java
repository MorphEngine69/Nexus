package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.access.PlayerPlaced;
import com.morphengine.nexus.api.network.security.Permission;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Placement and right click handling shared by every block whose block entity
 * is a {@link MenuHost}.
 */
public final class MenuHosts {

    private MenuHosts() {
    }

    /**
     * Tells the block entity at {@code pos} it was just placed, so it ignores
     * the held button, and on the server by whom, when it wants to know.
     *
     * @param placer whoever placed it; {@code null} when nobody did, as for a block set by a command
     */
    public static void placed(final Level level, final BlockPos pos, final @Nullable LivingEntity placer) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuHost host) {
            host.markPlaced();
        }
        if (!level.isClientSide() && placer instanceof Player player && blockEntity instanceof PlayerPlaced placed) {
            placed.placedBy(player);
        }
    }

    /**
     * Opens the menu of the block entity at {@code pos} on the server for a
     * player who may open it in its network; the menu reads the position from
     * the buffer written here, then any data the host adds.
     */
    public static InteractionResult open(final Level level, final BlockPos pos, final Player player) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof MenuHost host) || host.ignoresClick()) {
            return InteractionResult.SUCCESS;
        }
        if (NetworkAccess.permits(player, host, Permission.OPEN)) {
            show(player, host, pos);
        } else {
            NetworkAccess.refuse(player, Permission.OPEN);
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
