package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.filter.FilterKinds;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.networking.TransferSettingsPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.terminal.EnumCycle;
import com.morphengine.nexus.transfer.DeliveryMode;
import com.morphengine.nexus.transfer.TransferKind;
import com.morphengine.nexus.transfer.TransferSettings;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Puller or Pusher panel: the filter, the upgrade slots right of it, the
 * player's inventory, and the buttons for the resource moved, the redstone
 * mode, on a Pusher the order of delivery, and with a Regulator Upgrade the
 * amounts to keep in stock. Mode buttons go
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
    public static final int BUTTON_RESOURCE = 6;
    public static final int BUTTON_MATCH_MODE = 8;
    private static final int BUTTON_IDS = 10;

    private static final int SLOT_SPACING = 18;

    private final TransferKind kind;
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    /** The device's upgrades on the server; on the client, a copy the slots keep in step. */
    private final Container upgrades;
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
        this.upgrades = viewer() != null && device != null ? device.upgrades() : new UpgradeContainer(
                TransferDeviceBlockEntity.UPGRADE_SLOTS, TransferDeviceBlockEntity.UPGRADE_LIMITS, () -> { });
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
        return TransferDeviceBlockEntity.filterSlotCount(upgrades);
    }

    /**
     * @return what the filter lists for the resource the device moves; nothing
     *         for energy, whose filter is locked
     */
    @Override
    public FilterKinds filterKinds() {
        return settings().resource().filterKinds();
    }

    @Override
    public void changeFilter(final FilterSlots changed) {
        final TransferDeviceBlockEntity device = blockEntity();
        if (device != null) {
            device.changeSettings(device.settings().withFilter(changed));
        }
    }

    /**
     * Sets how much of the resource in filter slot {@code slot} the device keeps
     * in stock. An empty slot changes nothing. Server side only.
     */
    public void setKeepAmount(final int slot, final long amount) {
        final TransferDeviceBlockEntity device = blockEntity();
        if (device == null) {
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
            case BUTTON_SCHEDULING -> kind.hasScheduling()
                    ? current.withScheduling(EnumCycle.step(current.scheduling(), backwards)) : current;
            case BUTTON_DELIVERY -> current.withDelivery(EnumCycle.step(current.delivery(), backwards));
            case BUTTON_RESOURCE -> current.withResource(EnumCycle.step(current.resource(), backwards));
            case BUTTON_MATCH_MODE -> current.withMatchMode(EnumCycle.step(current.matchMode(), backwards));
            default -> current;
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
        if (slotIndex >= upgradeSlots && !TransferDeviceBlockEntity.UPGRADE_LIMITS.takesKindOf(stack)) {
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
     * @return whether the device may keep stock of what its whitelist lists:
     *         it holds a Regulator Upgrade and its filter is a whitelist
     */
    public boolean regulates() {
        return UpgradeLimits.count(upgrades, UpgradeTypes.REGULATOR.get()) > 0
                && settings().filter().mode() == FilterMode.ALLOW;
    }

    /**
     * @return whether the device keeps stock now, so the filter slots show and
     *         edit the amounts kept
     */
    public boolean showsKeepAmounts() {
        return regulates() && settings().delivery() == DeliveryMode.KEEP_STOCKED;
    }

    /**
     * @return whether the order of delivery applies: a Pusher whose whitelist
     *         lists items or fluids to choose from
     */
    public boolean showsScheduling() {
        return kind.hasScheduling() && settings().filter().mode() == FilterMode.ALLOW
                && filterKinds().listsAnything();
    }
}
