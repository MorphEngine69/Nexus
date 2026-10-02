package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.DeviceUpgrades;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The column of upgrade slots of a panel, over the slots of the device on the
 * server and over stand-ins on the client, which the server fills.
 */
final class UpgradeColumn {

    private static final int SLOT_SPACING = 18;

    private UpgradeColumn() {
    }

    /**
     * @param hosted the device's slots; {@code null} on the client
     * @return the slots of the column, top to bottom
     */
    static List<Slot> slots(final @Nullable Container hosted, final int left, final int top) {
        final Container upgrades = hosted != null ? hosted
                : new UpgradeContainer(DeviceUpgrades.SIZE, DeviceUpgrades.LIMITS, () -> { });
        final List<Slot> column = new ArrayList<>(DeviceUpgrades.SIZE);
        for (int slot = 0; slot < DeviceUpgrades.SIZE; slot++) {
            column.add(new UpgradeSlot(upgrades, slot, left, top + slot * SLOT_SPACING));
        }
        return column;
    }

    static boolean takes(final ItemStack stack) {
        return DeviceUpgrades.LIMITS.takesKindOf(stack);
    }
}
