package com.morphengine.nexus.menu;

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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Storage Vault panel: the cell slots, the vault's priority with buttons to
 * change it, and the player's inventory. Priority buttons go through the
 * vanilla menu button packet; the priority returns in a data slot.
 */
public final class StorageVaultMenu extends DeviceMenu<StorageVaultBlockEntity> implements NetworkBadgeView {

    /** Four by four, as on the vault's front. */
    public static final int CELL_COLUMNS = 4;
    public static final int CELLS_LEFT = 64;
    public static final int CELLS_TOP = 56;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 146;

    /** Menu button ids; the change they make to the priority is {@link #PRIORITY_STEPS}. */
    public static final int BUTTON_LOWER_TEN = 0;
    public static final int BUTTON_LOWER = 1;
    public static final int BUTTON_RAISE = 2;
    public static final int BUTTON_RAISE_TEN = 3;

    private static final int[] PRIORITY_STEPS = {-10, -1, 1, 10};
    private static final int SLOT_SPACING = 18;

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
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
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
    public boolean clickMenuButton(final Player player, final int buttonId) {
        final StorageVaultBlockEntity vault = blockEntity();
        if (vault == null || buttonId < 0 || buttonId >= PRIORITY_STEPS.length) {
            return false;
        }
        vault.setPriority(vault.storagePriority() + PRIORITY_STEPS[buttonId]);
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
        final Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        final int cellSlots = StorageVaultBlockEntity.SLOTS;
        final boolean moved = slotIndex < cellSlots
                ? moveItemStackTo(stack, cellSlots, slots.size(), true)
                : stack.getItem() instanceof VaultCellItem && moveItemStackTo(stack, 0, cellSlots, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
