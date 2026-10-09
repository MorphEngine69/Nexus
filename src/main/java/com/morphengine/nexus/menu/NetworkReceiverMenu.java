package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.NetworkReceiverBlockEntity;
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
 * Network Receiver panel: the slot for a Chunk Loader Upgrade and the player's
 * inventory.
 */
public final class NetworkReceiverMenu extends DeviceMenu<NetworkReceiverBlockEntity> implements NetworkBadgeView {

    public static final int UPGRADE_SLOT_X = 20;
    public static final int UPGRADE_SLOT_Y = 42;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 78;

    private static final int PLAYER_SLOTS_START = NetworkReceiverBlockEntity.UPGRADE_SLOTS;

    private final Container upgrades;
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    private @Nullable NetworkBadge badge;

    public NetworkReceiverMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.NETWORK_RECEIVER.get(), containerId, inventory, pos, NetworkReceiverBlockEntity.class);
        final NetworkReceiverBlockEntity receiver = blockEntity();
        this.upgrades = viewer() != null && receiver != null ? receiver.upgrades() : new UpgradeContainer(
                NetworkReceiverBlockEntity.UPGRADE_SLOTS, NetworkReceiverBlockEntity.UPGRADE_LIMITS, () -> { });
        addSlot(new UpgradeSlot(upgrades, 0, UPGRADE_SLOT_X, UPGRADE_SLOT_Y));
        InventorySlots.add(this::addSlot, inventory, INVENTORY_LEFT, INVENTORY_TOP);
    }

    /**
     * @return whether the receiver holds a Chunk Loader Upgrade now
     */
    public boolean holdsChunkLoader() {
        return UpgradeLimits.count(upgrades, UpgradeTypes.CHUNK_LOADER.get()) > 0;
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
        final NetworkReceiverBlockEntity receiver = blockEntity();
        final ServerPlayer viewer = viewer();
        if (receiver != null && viewer != null) {
            badgeSync.tick(viewer, containerId, receiver.networkBadge());
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
                : NetworkReceiverBlockEntity.UPGRADE_LIMITS.takesKindOf(stack)
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
