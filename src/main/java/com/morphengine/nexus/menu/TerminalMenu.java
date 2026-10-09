package com.morphengine.nexus.menu;

import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalLayout;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
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

    /**
     * @param type the menu type of a terminal block or of a Nexus Terminal
     */
    public TerminalMenu(
            final MenuType<?> type, final int containerId, final Inventory inventory, final TerminalOpening opening) {
        super(type, containerId);
        this.terminal = new TerminalMenuState(opening, TerminalKind.TERMINAL, containerId);
        InventorySlots.add(this::addSlot, inventory, 0, 0);
        addDataSlot(terminal.access());
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
    public void clicked(final int slotIndex, final int buttonNum, final ClickType input, final Player player) {
        if (SlotGuard.allows(this, slotIndex, input, player)) {
            super.clicked(slotIndex, buttonNum, input, player);
        }
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
