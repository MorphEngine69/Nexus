package com.morphengine.nexus.menu;

import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalLayout;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Terminal panel: the network's storage above the player's inventory. The
 * storage is not made of slots; clicks on it reach the server as payloads.
 * Shift-clicking an inventory slot stores its items in the network. The
 * screen places the slots through {@link #layOut}.
 */
public final class TerminalMenu extends AbstractContainerMenu implements TerminalPanel {

    private final TerminalMenuState terminal;

    public TerminalMenu(
            final int containerId, final Inventory inventory, final BlockPos pos, final TerminalSettings settings) {
        super(NexusMenuTypes.TERMINAL.get(), containerId);
        this.terminal = new TerminalMenuState(inventory, pos, TerminalKind.TERMINAL, settings, containerId);
        addStandardInventorySlots(inventory, 0, 0);
    }

    @Override
    public TerminalMenuState terminal() {
        return terminal;
    }

    @Override
    public void layOut(final TerminalLayout layout) {
        InventorySlots.place(slots, 0, layout.inventoryLeft(), layout.inventoryTop());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        terminal.tick();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final Slot slot = slots.get(slotIndex);
        if (slot.hasItem() && player instanceof ServerPlayer serverPlayer) {
            terminal.insert(serverPlayer, this, slot.getItem());
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return terminal.binding().stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        terminal.close();
    }
}
