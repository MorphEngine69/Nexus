package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
import com.morphengine.nexus.external.ExternalVaultSettings;
import com.morphengine.nexus.filter.FilterKinds;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.networking.ExternalVaultSettingsPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.terminal.EnumCycle;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * External Vault panel: the priority of the vault with buttons to change it, the filter of what the network may use,
 * what the network may do with it, and the inventory of the player. The filter slots are ghosts, see
 * {@link FilterMenu}. Buttons go through the vanilla menu button packet; the priority returns in a data slot and the
 * settings in a payload of their own whenever they change.
 */
public final class ExternalVaultMenu extends DeviceMenu<ExternalVaultBlockEntity>
        implements FilterMenu, NetworkBadgeView {

    public static final int FILTER_LEFT = 19;
    public static final int FILTER_TOP = 82;
    public static final int FILTER_COLUMNS = 9;
    public static final int UPGRADES_LEFT = 196;
    public static final int UPGRADES_TOP = FILTER_TOP;
    public static final int INVENTORY_LEFT = 19;
    /** Four rows of filter slots, the most that Capacity Upgrades give, fit above it. */
    public static final int INVENTORY_TOP = 168;

    /** Menu button id of the first {@link PriorityButtons priority button}. */
    public static final int BUTTON_PRIORITY = 0;
    public static final int BUTTON_ACCESS = PriorityButtons.count();

    private static final int SLOT_SPACING = 18;

    private final DataSlot priority = DataSlot.standalone();
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    /** The upgrades of the vault on the server; on the client, a copy the slots keep in step. */
    private final Container upgrades;
    /** On the client, the settings last received; on the server, the settings last sent. */
    private ExternalVaultSettings settings;
    private @Nullable NetworkBadge badge;

    public ExternalVaultMenu(
            final int containerId, final Inventory inventory, final BlockPos pos,
            final ExternalVaultSettings settings) {
        super(NexusMenuTypes.EXTERNAL_VAULT.get(), containerId, inventory, pos, ExternalVaultBlockEntity.class);
        this.settings = settings;
        final ExternalVaultBlockEntity vault = blockEntity();
        this.upgrades = viewer() != null && vault != null ? vault.upgrades() : new UpgradeContainer(
                ExternalVaultBlockEntity.UPGRADE_SLOTS, ExternalVaultBlockEntity.UPGRADE_LIMITS, () -> { });
        for (int slot = 0; slot < ExternalVaultBlockEntity.UPGRADE_SLOTS; slot++) {
            addSlot(new UpgradeSlot(upgrades, slot, UPGRADES_LEFT, UPGRADES_TOP + slot * SLOT_SPACING));
        }
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
        addDataSlot(priority);
    }

    public int priority() {
        return priority.get();
    }

    /**
     * @return the settings of the vault; on the client, as last received
     */
    public ExternalVaultSettings settings() {
        final ExternalVaultBlockEntity vault = blockEntity();
        return viewer() != null && vault != null ? vault.settings() : settings;
    }

    public void acceptSettings(final ExternalVaultSettings received) {
        settings = received;
    }

    @Override
    public FilterSlots filter() {
        return settings().filter();
    }

    @Override
    public int filterSlotCount() {
        return ExternalVaultBlockEntity.filterSlotCount(upgrades);
    }

    @Override
    public FilterKinds filterKinds() {
        return FilterKinds.ITEMS_AND_FLUIDS;
    }

    @Override
    public boolean listsTags() {
        return true;
    }

    @Override
    public void changeFilter(final FilterSlots changed) {
        final ExternalVaultBlockEntity vault = blockEntity();
        if (vault != null) {
            vault.changeSettings(vault.settings().withFilter(changed));
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
    protected boolean pressButton(final Player player, final int buttonId) {
        final ExternalVaultBlockEntity vault = blockEntity();
        if (vault == null) {
            return false;
        }
        final int index = buttonId - BUTTON_PRIORITY;
        if (index >= 0 && index < PriorityButtons.count()) {
            vault.setPriority(vault.storagePriority() + PriorityButtons.stepOf(index));
            return true;
        }
        if (buttonId == BUTTON_ACCESS) {
            vault.changeSettings(vault.settings().withAccess(EnumCycle.step(vault.settings().access(), false)));
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        final ExternalVaultBlockEntity vault = blockEntity();
        final ServerPlayer viewer = viewer();
        if (vault != null && viewer != null) {
            priority.set(vault.storagePriority());
            if (vault.settings() != settings) {
                settings = vault.settings();
                PacketDistributor.sendToPlayer(viewer, new ExternalVaultSettingsPayload(containerId, settings));
            }
            badgeSync.tick(viewer, containerId, vault.networkBadge());
        }
        super.broadcastChanges();
    }

    /**
     * Shift click on an upgrade moves it between the inventory and the upgrade slots; on anything else it lists
     * the item in the filter and nothing moves.
     */
    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final ItemStack stack = slots.get(slotIndex).getItem();
        final int upgradeSlots = ExternalVaultBlockEntity.UPGRADE_SLOTS;
        if (slotIndex >= upgradeSlots && !ExternalVaultBlockEntity.UPGRADE_LIMITS.takesKindOf(stack)) {
            if (!player.level().isClientSide()) {
                addToFilter(stack);
            }
            return ItemStack.EMPTY;
        }
        return shiftClick(slotIndex, upgradeSlots, 0, upgradeSlots);
    }
}
