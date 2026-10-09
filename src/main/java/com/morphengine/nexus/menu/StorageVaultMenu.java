package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.DeviceUpgrades;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Storage Vault panel: the cell slots, the upgrade slots, the
 * vault's priority with buttons to change it, and the player's inventory.
 * Priority buttons go through the vanilla menu button packet; the priority
 * returns in a data slot.
 */
public final class StorageVaultMenu extends DeviceMenu<StorageVaultBlockEntity> implements NetworkBadgeView {

    /** Four by four, as on the vault's front. */
    public static final int CELL_COLUMNS = 4;
    public static final int CELLS_LEFT = 64;
    public static final int CELLS_TOP = 52;
    public static final int UPGRADES_LEFT = 172;
    public static final int UPGRADES_TOP = CELLS_TOP;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 142;

    /** Menu button id of the first {@link PriorityButtons priority button}. */
    public static final int BUTTON_PRIORITY = 0;

    private static final int SLOT_SPACING = 18;
    private static final int PANEL_SLOTS = StorageVaultBlockEntity.SLOTS + DeviceUpgrades.SIZE;

    private final DataSlot priority = DataSlot.standalone();
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    private @Nullable NetworkBadge badge;

    public StorageVaultMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.STORAGE_VAULT.get(), containerId, inventory, pos, StorageVaultBlockEntity.class);
        final StorageVaultBlockEntity vault = blockEntity();
        final Container cells = viewer() != null && vault != null
                ? vault.cells() : new SimpleContainer(StorageVaultBlockEntity.SLOTS);
        for (int slot = 0; slot < StorageVaultBlockEntity.SLOTS; slot++) {
            addSlot(new CellSlot(cells, slot, CELLS_LEFT + slot % CELL_COLUMNS * SLOT_SPACING,
                    CELLS_TOP + slot / CELL_COLUMNS * SLOT_SPACING));
        }
        UpgradeColumn.slots(viewer() != null && vault != null ? vault.upgrades() : null,
                StorageVaultBlockEntity.UPGRADE_LIMITS, UPGRADES_LEFT, UPGRADES_TOP)
                .forEach(this::addSlot);
        InventorySlots.add(this::addSlot, inventory, INVENTORY_LEFT, INVENTORY_TOP);
        addDataSlot(priority);
    }

    public int priority() {
        return priority.get();
    }

    @Override
    public @Nullable NetworkBadge badge() {
        return badge;
    }

    @Override
    public void acceptBadge(final @Nullable NetworkBadge received) {
        badge = received;
    }

    @Override
    protected boolean pressButton(final Player player, final int buttonId) {
        final StorageVaultBlockEntity vault = blockEntity();
        final int index = buttonId - BUTTON_PRIORITY;
        if (vault == null || index < 0 || index >= PriorityButtons.count()) {
            return false;
        }
        vault.setPriority(vault.storagePriority() + PriorityButtons.stepOf(index));
        return true;
    }

    @Override
    public void broadcastChanges() {
        final StorageVaultBlockEntity vault = blockEntity();
        final ServerPlayer viewer = viewer();
        if (vault != null && viewer != null) {
            priority.set(vault.storagePriority());
            badgeSync.tick(viewer, containerId, vault.networkBadge());
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final ItemStack stack = slots.get(slotIndex).getItem();
        final int cellSlots = StorageVaultBlockEntity.SLOTS;
        if (stack.getItem() instanceof VaultCellItem) {
            return shiftClick(slotIndex, PANEL_SLOTS, 0, cellSlots);
        }
        final boolean isUpgrade = UpgradeColumn.takes(StorageVaultBlockEntity.UPGRADE_LIMITS, stack);
        return shiftClick(slotIndex, PANEL_SLOTS, cellSlots, isUpgrade ? PANEL_SLOTS : cellSlots);
    }
}
