package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.MenuHost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Base for the menu of a network device: a panel without slots, bound to the
 * block entity at one position. Designed for extension; subclasses add what the
 * panel shows and how it reaches the client.
 *
 * @param <B> the device's block entity
 */
public abstract class DeviceMenu<B extends BlockEntity & MenuHost> extends AbstractContainerMenu
        implements DevicePanel {

    private final DeviceBinding<B> binding;

    protected DeviceMenu(
            final MenuType<?> type, final int containerId, final Inventory inventory, final BlockPos pos,
            final Class<B> blockEntityType) {
        super(type, containerId);
        this.binding = new DeviceBinding<>(inventory, pos, blockEntityType);
    }

    @Override
    public DeviceBinding<B> binding() {
        return binding;
    }

    public BlockPos pos() {
        return binding.pos();
    }

    /**
     * @return the device's block entity; {@code null} if it was gone when the menu opened
     */
    public @Nullable B blockEntity() {
        return binding.blockEntity();
    }

    /**
     * @return the player the panel data is sent to; {@code null} on the client
     */
    protected @Nullable ServerPlayer viewer() {
        return binding.viewer();
    }

    @Override
    public boolean stillValid(final Player player) {
        return binding.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        binding.release();
    }
}
