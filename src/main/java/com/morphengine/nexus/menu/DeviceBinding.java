package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.MenuHost;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * What ties a menu to the device it was opened on: the position, the block
 * entity found there when the menu opened, and on the server the player it is
 * shown to. A menu owns one and delegates to it.
 *
 * @param <B> the device's block entity
 */
public final class DeviceBinding<B extends BlockEntity & MenuHost> {

    /** The reach vanilla containers allow. */
    private static final double REACH = 4.0;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final @Nullable B blockEntity;
    private final @Nullable ServerPlayer viewer;

    public DeviceBinding(final Inventory inventory, final BlockPos pos, final Class<B> blockEntityType) {
        this.pos = Objects.requireNonNull(pos, "pos must not be null");
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        final BlockEntity found = inventory.player.level().getBlockEntity(pos);
        this.blockEntity = blockEntityType.isInstance(found) ? blockEntityType.cast(found) : null;
        this.viewer = inventory.player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    public BlockPos pos() {
        return pos;
    }

    /**
     * @return the device's block entity; {@code null} if it was gone when the menu opened
     */
    public @Nullable B blockEntity() {
        return blockEntity;
    }

    /**
     * @return the player the panel data is sent to; {@code null} on the client
     */
    public @Nullable ServerPlayer viewer() {
        return viewer;
    }

    public ContainerLevelAccess access() {
        return access;
    }

    public boolean stillValid(final Player player) {
        return blockEntity != null && access.evaluate((level, at) -> level.getBlockState(at).is(
                blockEntity.getBlockState().getBlock()) && player.isWithinBlockInteractionRange(at, REACH), true);
    }

    /**
     * Tells the device its menu was closed, when the menu is removed.
     */
    public void release() {
        if (blockEntity != null) {
            blockEntity.markClosed();
        }
    }

    public Component defaultTitle() {
        return blockEntity != null ? blockEntity.getBlockState().getBlock().getName() : Component.empty();
    }
}
