package com.morphengine.nexus.item;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.NetworkBlock;
import com.morphengine.nexus.block.Turnable;
import com.morphengine.nexus.level.NetworkChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * What the Wrench does to a block of a network. The client only answers whether the click is the Wrench's, so that the
 * hand swings; the server does the work.
 */
public final class WrenchActions {

    private static final double PICKUP_REACH = 1.5;

    private WrenchActions() {
    }

    /**
     * Turns the front of the block at {@code pos}.
     *
     * @param side the side to set the front to; {@code null} to turn it to the next one
     * @return {@code PASS} for a block with no front to turn, so that the click goes on to the block itself;
     *         {@code FAIL} when the block cannot face {@code side}, or the player may not change it
     */
    public static InteractionResult turn(
            final Level level, final BlockPos pos, final Player player, final @Nullable Direction side) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof Turnable turnable)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final BlockState faced = side == null ? turnable.turned(state) : turnable.facing(state, side);
        if (faced == null || faced.equals(state) || !mayConfigure(level, pos, player)) {
            return InteractionResult.FAIL;
        }
        level.setBlock(pos, turnable.withPorts(faced, level, pos), Block.UPDATE_ALL);
        NetworkChanges.blockTurned(level, pos);
        return InteractionResult.SUCCESS;
    }

    private static boolean mayConfigure(final Level level, final BlockPos pos, final Player player) {
        final boolean permitted = NetworkAccess.permitsOrOwns(player, level.getBlockEntity(pos), Permission.CONFIGURE);
        if (!permitted) {
            NetworkAccess.refuse(player, Permission.CONFIGURE);
        }
        return permitted;
    }

    /**
     * Breaks the block at {@code pos} as the player would, under the same rules, and puts what it drops into their
     * inventory; what does not fit lands beside the block.
     */
    public static InteractionResult dismantle(final Level level, final BlockPos pos, final Player player) {
        if (!(level.getBlockState(pos).getBlock() instanceof NetworkBlock)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!serverPlayer.gameMode.destroyBlock(pos)) {
            return InteractionResult.FAIL;
        }
        collectDrops(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    /**
     * Takes the items that fell out of the block at {@code pos} this tick: a drop is a fresh item entity, so one
     * lying there already is left alone.
     */
    private static void collectDrops(final Level level, final BlockPos pos, final Player player) {
        final AABB around = new AABB(pos).inflate(PICKUP_REACH);
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, around, entity -> entity.tickCount == 0)) {
            final ItemStack stack = drop.getItem();
            player.getInventory().add(stack);
            if (stack.isEmpty()) {
                drop.discard();
            }
        }
    }
}
