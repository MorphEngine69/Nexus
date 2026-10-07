package com.morphengine.nexus.menu;

import com.morphengine.nexus.filter.FilterKinds;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.item.VoidUpgradeItem;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Panel of the Void Upgrade held in the main hand: the items and fluids the network destroys instead of storing. The
 * list is only ever a whitelist, so there is no switch to a blacklist.
 */
public final class VoidUpgradeMenu extends HeldFilterMenu implements PanelMenu {

    public VoidUpgradeMenu(final int containerId, final Inventory inventory) {
        super(NexusMenuTypes.VOID_UPGRADE.get(), containerId, inventory);
    }

    @Override
    public FilterSlots filter() {
        return VoidUpgradeItem.listOf(held());
    }

    @Override
    public FilterKinds filterKinds() {
        return FilterKinds.ITEMS_AND_FLUIDS;
    }

    @Override
    public void changeFilter(final FilterSlots changed) {
        if (holdsItem()) {
            VoidUpgradeItem.setList(held(), changed);
        }
    }

    @Override
    public void toggleFilterMode() {
        // A list of what to destroy has no other mode.
    }

    @Override
    public Component defaultTitle() {
        return held().getItem().getName(held());
    }

    @Override
    protected boolean holdsItem() {
        return held().getItem() instanceof VoidUpgradeItem;
    }
}
