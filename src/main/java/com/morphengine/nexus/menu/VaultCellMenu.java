package com.morphengine.nexus.menu;

import com.morphengine.nexus.item.CellFilter;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.networking.CellRenamePayload;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Panel of the Vault Cell held in the main hand: its usage, its filter and its
 * name. The filter slots are ghosts: clicking one with an item or a filled
 * container lists that resource without taking anything. The held cell's slot
 * is locked while the panel is open.
 */
public final class VaultCellMenu extends AbstractContainerMenu implements PanelMenu {

    public static final int FILTER_LEFT = 19;
    public static final int FILTER_TOP = 66;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 108;

    private static final int SLOT_SPACING = 18;
    private static final int INVENTORY_ROWS = 3;
    private static final int ROW_LENGTH = 9;
    private static final int HOTBAR_OFFSET = 58;

    private final Player player;
    private final int heldSlot;

    public VaultCellMenu(final int containerId, final Inventory inventory) {
        super(NexusMenuTypes.VAULT_CELL.get(), containerId);
        this.player = inventory.player;
        this.heldSlot = inventory.getSelectedSlot();
        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int column = 0; column < ROW_LENGTH; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * ROW_LENGTH,
                        INVENTORY_LEFT + column * SLOT_SPACING, INVENTORY_TOP + row * SLOT_SPACING));
            }
        }
        for (int column = 0; column < ROW_LENGTH; column++) {
            final int x = INVENTORY_LEFT + column * SLOT_SPACING;
            final int y = INVENTORY_TOP + HOTBAR_OFFSET;
            addSlot(column == heldSlot ? new LockedSlot(inventory, column, x, y) : new Slot(inventory, column, x, y));
        }
    }

    /**
     * @return the cell the panel edits, in the main hand
     */
    public ItemStack cell() {
        return player.getInventory().getItem(heldSlot);
    }

    public @Nullable VaultCellItem cellItem() {
        return cell().getItem() instanceof VaultCellItem item ? item : null;
    }

    public CellFilter filter() {
        return VaultCellItem.filterOf(cell());
    }

    /**
     * Lists {@code resource} in filter slot {@code slot}, or empties the slot.
     * A resource the cell cannot store is ignored. Server side only.
     */
    public void setFilterSlot(final int slot, final @Nullable NexusResource resource) {
        final VaultCellItem item = cellItem();
        final boolean fits = resource == null || item != null && resource.type() == item.kind().resourceType();
        if (item == null || slot < 0 || slot >= CellFilter.SLOTS || !fits) {
            return;
        }
        cell().set(NexusDataComponents.CELL_FILTER.get(), filter().with(slot, resource));
    }

    /**
     * Switches the filter between whitelist and blacklist. Server side only.
     */
    public void toggleFilterMode() {
        if (cellItem() != null) {
            cell().set(NexusDataComponents.CELL_FILTER.get(), filter().withMode(filter().mode().toggled()));
        }
    }

    /**
     * @param name the new name of the cell; blank restores the default one. Server side only.
     */
    public void rename(final String name) {
        final String stripped = name.strip();
        if (cellItem() == null || stripped.length() > VaultCellItem.MAX_NAME_LENGTH) {
            return;
        }
        if (stripped.isEmpty()) {
            cell().remove(DataComponents.CUSTOM_NAME);
        } else {
            cell().set(DataComponents.CUSTOM_NAME, Component.literal(stripped));
        }
    }

    @Override
    public CustomPacketPayload renamePayload(final String name) {
        return new CellRenamePayload(containerId, name);
    }

    @Override
    public Component defaultTitle() {
        return cell().getItem().getName(cell());
    }

    @Override
    public void clicked(final int slotIndex, final int buttonNum, final ContainerInput input, final Player clicker) {
        if (input == ContainerInput.SWAP && buttonNum == heldSlot) {
            return;
        }
        super.clicked(slotIndex, buttonNum, input, clicker);
    }

    @Override
    public ItemStack quickMoveStack(final Player clicker, final int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player clicker) {
        return cellItem() != null;
    }
}
