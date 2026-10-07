package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * A terminal menu opened on a terminal block: the block entity found at the
 * position is the host, and the menu stays open while the player is near it.
 */
public final class BlockTerminalBinding implements TerminalBinding {

    private final DeviceBinding<TerminalBlockEntity> binding;

    public BlockTerminalBinding(final Inventory inventory, final BlockPos pos) {
        this.binding = new DeviceBinding<>(inventory, pos, TerminalBlockEntity.class);
    }

    @Override
    public @Nullable TerminalHost host() {
        return binding.viewer() != null ? binding.blockEntity() : null;
    }

    @Override
    public @Nullable ServerPlayer viewer() {
        return binding.viewer();
    }

    @Override
    public boolean stillValid(final Player player) {
        return binding.stillValid(player);
    }

    @Override
    public boolean permits(final Player player, final Permission permission) {
        return binding.permits(player, permission);
    }

    @Override
    public void release() {
        binding.release();
    }

    @Override
    public Component defaultTitle() {
        return binding.defaultTitle();
    }
}
