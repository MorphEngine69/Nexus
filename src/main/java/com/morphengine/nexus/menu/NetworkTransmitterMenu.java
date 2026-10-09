package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.NetworkTransmitterBlockEntity;
import com.morphengine.nexus.block.entity.TransmitterStatus;
import com.morphengine.nexus.item.NetworkCardItem;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
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
 * Network Transmitter panel: the slot for its Network Card, what becomes of
 * the link, the slot for a Chunk Loader Upgrade, and the player's inventory.
 * The status returns in a data slot; the card, and with it where the link
 * goes, in its slot.
 */
public final class NetworkTransmitterMenu extends DeviceMenu<NetworkTransmitterBlockEntity>
        implements NetworkBadgeView {

    public static final int CARD_SLOT_X = 20;
    public static final int CARD_SLOT_Y = 42;
    public static final int UPGRADE_SLOT_Y = CARD_SLOT_Y + 22;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 100;

    private static final int CARD_SLOT = 0;
    private static final int UPGRADE_SLOT = 1;
    private static final int PLAYER_SLOTS_START = 2;

    private final Container upgrades;
    private final DataSlot status = DataSlot.standalone();
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    private @Nullable NetworkBadge badge;

    public NetworkTransmitterMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.NETWORK_TRANSMITTER.get(), containerId, inventory, pos,
                NetworkTransmitterBlockEntity.class);
        final NetworkTransmitterBlockEntity transmitter = blockEntity();
        final Container card = viewer() != null && transmitter != null ? transmitter.card() : new SimpleContainer(1);
        addSlot(new CardSlot(card));
        this.upgrades = viewer() != null && transmitter != null ? transmitter.upgrades() : new UpgradeContainer(
                NetworkTransmitterBlockEntity.UPGRADE_SLOTS, NetworkTransmitterBlockEntity.UPGRADE_LIMITS, () -> { });
        addSlot(new UpgradeSlot(upgrades, 0, CARD_SLOT_X, UPGRADE_SLOT_Y));
        InventorySlots.add(this::addSlot, inventory, INVENTORY_LEFT, INVENTORY_TOP);
        addDataSlot(status);
    }

    public TransmitterStatus status() {
        final TransmitterStatus[] values = TransmitterStatus.values();
        return values[Math.floorMod(status.get(), values.length)];
    }

    /**
     * @return the Network Card in the slot; empty when there is none
     */
    public ItemStack card() {
        return slots.get(CARD_SLOT).getItem();
    }

    /**
     * @return whether the transmitter holds a Chunk Loader Upgrade now
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
        final NetworkTransmitterBlockEntity transmitter = blockEntity();
        final ServerPlayer viewer = viewer();
        if (transmitter != null && viewer != null) {
            status.set(transmitter.status().ordinal());
            badgeSync.tick(viewer, containerId, transmitter.networkBadge());
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
                : moveIntoTransmitter(stack);
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

    private boolean moveIntoTransmitter(final ItemStack stack) {
        if (stack.getItem() instanceof NetworkCardItem) {
            return moveItemStackTo(stack, CARD_SLOT, UPGRADE_SLOT, false);
        }
        return NetworkTransmitterBlockEntity.UPGRADE_LIMITS.takesKindOf(stack)
                && moveItemStackTo(stack, UPGRADE_SLOT, PLAYER_SLOTS_START, false);
    }

    private static final class CardSlot extends Slot {

        CardSlot(final Container container) {
            super(container, 0, CARD_SLOT_X, CARD_SLOT_Y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return stack.getItem() instanceof NetworkCardItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
