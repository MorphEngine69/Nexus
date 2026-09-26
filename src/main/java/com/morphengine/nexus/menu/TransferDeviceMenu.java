package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.filter.FilterKinds;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.networking.TransferSettingsPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.registry.NexusTags;
import com.morphengine.nexus.terminal.EnumCycle;
import com.morphengine.nexus.transfer.DeliveryMode;
import com.morphengine.nexus.transfer.TransferKind;
import com.morphengine.nexus.transfer.TransferSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Puller or Pusher panel: the filter, the upgrade slots right of it, the
 * player's inventory, and the buttons for the redstone mode and, on a Pusher,
 * the order of delivery and the amounts to keep stocked. Mode buttons go
 * through the vanilla menu button packet; the settings reach the client when
 * the panel opens and again whenever they change.
 */
public final class TransferDeviceMenu extends DeviceMenu<TransferDeviceBlockEntity>
        implements FilterMenu, NetworkBadgeView {

    public static final int FILTER_LEFT = 19;
    public static final int FILTER_TOP = 46;
    public static final int FILTER_COLUMNS = 9;
    public static final int UPGRADES_LEFT = 196;
    public static final int UPGRADES_TOP = FILTER_TOP;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 132;

    /** Menu button ids; each mode button steps forwards with one and backwards with the next. */
    public static final int BUTTON_REDSTONE = 0;
    public static final int BUTTON_SCHEDULING = 2;
    public static final int BUTTON_DELIVERY = 4;
    private static final int BUTTON_IDS = 6;

    private static final int SLOT_SPACING = 18;

    private final TransferKind kind;
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    /** On the client, the settings last received; on the server, the settings last sent. */
    private TransferSettings settings;
    private @Nullable NetworkBadge badge;

    public TransferDeviceMenu(
            final int containerId, final Inventory inventory, final BlockPos pos, final TransferKind kind,
            final TransferSettings settings) {
        super(NexusMenuTypes.TRANSFER_DEVICE.get(), containerId, inventory, pos, TransferDeviceBlockEntity.class);
        this.kind = kind;
        this.settings = settings;
        final TransferDeviceBlockEntity device = blockEntity();
        final Container upgrades = viewer() != null && device != null
                ? device.upgrades() : new SimpleContainer(TransferDeviceBlockEntity.UPGRADE_SLOTS);
        for (int slot = 0; slot < TransferDeviceBlockEntity.UPGRADE_SLOTS; slot++) {
            addSlot(new UpgradeSlot(upgrades, slot, UPGRADES_LEFT, UPGRADES_TOP + slot * SLOT_SPACING));
        }
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
    }

    public TransferKind kind() {
        return kind;
    }

    /**
     * @return the device's settings; on the client, as last received
     */
    public TransferSettings settings() {
        final TransferDeviceBlockEntity device = blockEntity();
        return viewer() != null && device != null ? device.settings() : settings;
    }

    public void acceptSettings(final TransferSettings received) {
        settings = received;
    }

    @Override
    public FilterSlots filter() {
        return settings().filter();
    }

    @Override
    public int filterSlotCount() {
        return TransferDeviceBlockEntity.FILTER_SLOTS;
    }

    @Override
    public FilterKinds filterKinds() {
        return FilterKinds.ITEMS_AND_FLUIDS;
    }

    @Override
    public void changeFilter(final FilterSlots changed) {
        final TransferDeviceBlockEntity device = blockEntity();
        if (device != null) {
            device.changeSettings(device.settings().withFilter(changed));
        }
    }

    /**
     * Sets how much of the resource in filter slot {@code slot} a Pusher keeps
     * stocked. An empty slot or a device without such amounts changes nothing.
     * Server side only.
     */
    public void setKeepAmount(final int slot, final long amount) {
        final TransferDeviceBlockEntity device = blockEntity();
        if (device == null || !kind.hasDeliverySettings()) {
            return;
        }
        if (slot >= 0 && slot < filterSlotCount() && device.settings().filter().resourceAt(slot) != null) {
            device.changeSettings(device.settings().withKeepAmount(slot, amount));
        }
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
        final TransferDeviceBlockEntity device = blockEntity();
        if (device == null || buttonId < 0 || buttonId >= BUTTON_IDS) {
            return false;
        }
        final boolean backwards = buttonId % 2 == 1;
        final TransferSettings current = device.settings();
        final TransferSettings changed = switch (buttonId - buttonId % 2) {
            case BUTTON_REDSTONE -> current.withRedstone(EnumCycle.step(current.redstone(), backwards));
            case BUTTON_SCHEDULING -> kind.hasDeliverySettings()
                    ? current.withScheduling(EnumCycle.step(current.scheduling(), backwards)) : current;
            default -> kind.hasDeliverySettings()
                    ? current.withDelivery(EnumCycle.step(current.delivery(), backwards)) : current;
        };
        device.changeSettings(changed);
        return true;
    }

    @Override
    public void broadcastChanges() {
        final TransferDeviceBlockEntity device = blockEntity();
        final ServerPlayer viewer = viewer();
        if (device != null && viewer != null) {
            if (device.settings() != settings) {
                settings = device.settings();
                PacketDistributor.sendToPlayer(viewer, new TransferSettingsPayload(containerId, settings));
            }
            badgeSync.tick(viewer, containerId, device.networkBadge());
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
        final int upgradeSlots = TransferDeviceBlockEntity.UPGRADE_SLOTS;
        if (slotIndex >= upgradeSlots && !stack.is(NexusTags.UPGRADES)) {
            if (!player.level().isClientSide()) {
                addToFilter(stack);
            }
            return ItemStack.EMPTY;
        }
        final boolean moved = slotIndex < upgradeSlots
                ? moveItemStackTo(stack, upgradeSlots, slots.size(), true)
                : moveItemStackTo(stack, 0, upgradeSlots, false);
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

    /**
     * @return whether the settings shown keep amounts stocked, so the filter
     *         slots show and edit them: a Pusher with a whitelist set to it
     */
    public boolean showsKeepAmounts() {
        return deliversListed() && settings().delivery() == DeliveryMode.KEEP_STOCKED;
    }

    /**
     * @return whether the device delivers the resources its filter lists, so
     *         the order and amounts of delivery apply
     */
    public boolean deliversListed() {
        return kind.hasDeliverySettings() && settings().filter().mode() == FilterMode.ALLOW;
    }
}
