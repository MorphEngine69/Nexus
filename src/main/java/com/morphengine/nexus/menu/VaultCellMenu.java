package com.morphengine.nexus.menu;

import com.morphengine.nexus.filter.FilterKinds;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.networking.CellRenamePayload;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Panel of the Vault Cell held in the main hand: its usage, its filter and its
 * name. The filter slots are ghosts: clicking one with an item or a filled
 * container lists that resource without taking anything. The held cell's slot
 * is locked while the panel is open.
 */
public final class VaultCellMenu extends HeldFilterMenu implements RenamablePanel {

    public VaultCellMenu(final int containerId, final Inventory inventory) {
        super(NexusMenuTypes.VAULT_CELL.get(), containerId, inventory);
    }

    /**
     * @return the cell the panel edits, in the main hand
     */
    public ItemStack cell() {
        return held();
    }

    public @Nullable VaultCellItem cellItem() {
        return cell().getItem() instanceof VaultCellItem item ? item : null;
    }

    @Override
    public FilterSlots filter() {
        return VaultCellItem.filterOf(cell());
    }

    /**
     * @return the one kind of resource the held cell stores
     */
    @Override
    public FilterKinds filterKinds() {
        final VaultCellItem item = cellItem();
        return item != null ? item.kind().filterKinds() : FilterKinds.ITEMS;
    }

    @Override
    public void changeFilter(final FilterSlots changed) {
        if (cellItem() != null) {
            cell().set(NexusDataComponents.CELL_FILTER.get(), changed);
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
    protected boolean holdsItem() {
        return cellItem() != null;
    }
}
