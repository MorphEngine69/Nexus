package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Nexus Link panel: the slots for Range, Dimension and Chunk Loader Upgrades, how
 * far the link reaches with them, and the player's inventory. The range is worked
 * out from the slots on either side.
 */
public final class NexusLinkMenu extends DeviceMenu<NexusLinkBlockEntity> implements NetworkBadgeView {

    public static final int UPGRADE_SLOT_X = 20;
    public static final int UPGRADE_SLOT_Y = 42;
    public static final int UPGRADE_ROW_HEIGHT = 22;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 118;

    private static final int PLAYER_SLOTS_START = NexusLinkBlockEntity.UPGRADE_SLOTS;

    private final Container upgrades;
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    private @Nullable NetworkBadge badge;

    public NexusLinkMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.NEXUS_LINK.get(), containerId, inventory, pos, NexusLinkBlockEntity.class);
        final NexusLinkBlockEntity link = blockEntity();
        this.upgrades = viewer() != null && link != null ? link.upgrades() : new UpgradeContainer(
                NexusLinkBlockEntity.UPGRADE_SLOTS, NexusLinkBlockEntity.UPGRADE_LIMITS, () -> { });
        for (int slot = 0; slot < NexusLinkBlockEntity.UPGRADE_SLOTS; slot++) {
            addSlot(new UpgradeSlot(upgrades, slot, UPGRADE_SLOT_X, UPGRADE_SLOT_Y + slot * UPGRADE_ROW_HEIGHT));
        }
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
    }

    /**
     * @return whether the link holds a Chunk Loader Upgrade now
     */
    public boolean holdsChunkLoader() {
        return UpgradeLimits.count(upgrades, UpgradeTypes.CHUNK_LOADER.get()) > 0;
    }

    /**
     * @return whether the link holds a Dimension Upgrade now
     */
    public boolean holdsDimension() {
        return NexusLinkBlockEntity.reachesOtherDimensionsWith(upgrades);
    }

    /**
     * @return how far the link reaches with the upgrades it holds now, in blocks
     */
    public int range() {
        return NexusLinkBlockEntity.rangeWith(upgrades);
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
    public void broadcastChanges() {
        final NexusLinkBlockEntity link = blockEntity();
        final ServerPlayer viewer = viewer();
        if (link != null && viewer != null) {
            badgeSync.tick(viewer, containerId, link.networkBadge());
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
        final boolean moved = slotIndex < PLAYER_SLOTS_START
                ? moveItemStackTo(stack, PLAYER_SLOTS_START, slots.size(), true)
                : NexusLinkBlockEntity.UPGRADE_LIMITS.takesKindOf(stack)
                        && moveItemStackTo(stack, 0, PLAYER_SLOTS_START, false);
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
