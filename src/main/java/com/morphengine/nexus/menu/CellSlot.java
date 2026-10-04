package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.item.VaultCellItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * A Storage Vault slot: takes one Vault Cell. A cell carries what it stores,
 * so taking one out takes resources out of the network and putting one in
 * puts them in: the slot takes both, besides changing the vault.
 */
final class CellSlot extends Slot implements GuardedSlot {

    private static final Set<Permission> PERMISSIONS =
            Set.of(Permission.CONFIGURE, Permission.INSERT, Permission.EXTRACT);

    CellSlot(final Container container, final int index, final int x, final int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return stack.getItem() instanceof VaultCellItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public Set<Permission> permissions() {
        return PERMISSIONS;
    }
}
