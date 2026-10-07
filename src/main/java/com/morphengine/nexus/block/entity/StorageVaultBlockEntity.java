package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.VaultLamp;
import com.morphengine.nexus.item.VoidUpgradeItem;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.StorageHost;
import com.morphengine.nexus.level.UpgradeHolder;
import com.morphengine.nexus.menu.StorageVaultMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storage Vault: holds up to {@value VaultCellSlots#SIZE} Vault Cells and lends
 * them to its network at its priority. Once a second it saves the contents of
 * changed cells onto their items and shows on its meters how full each cell is;
 * with no energy in the network every meter is dark. Every {@value #BUSY_PERIOD_TICKS}
 * ticks it also shows which cells took or gave resources in that time. Cells that lost their slot
 * when the vault got fewer slots are dropped in front of it on its next tick. A Void Upgrade in its slots has the
 * network destroy what it lists.
 */
public final class StorageVaultBlockEntity extends AnimatedDeviceBlockEntity
        implements UpgradeHolder, StorageHost, Renamable, VaultCellSlots.Owner {

    public static final int SLOTS = VaultCellSlots.SIZE;
    /** One swell of a working cell's meter; the cells shown as working only change at its start, when it is at rest. */
    public static final int BUSY_PERIOD_TICKS = 72;

    /** A Chunk Loader Upgrade and a Void Upgrade, one of each. */
    public static final UpgradeLimits UPGRADE_LIMITS = new UpgradeLimits(
            Map.of(UpgradeTypes.CHUNK_LOADER, 1, UpgradeTypes.VOID, 1));

    private static final String TAG_PRIORITY = "priority";
    private static final String TAG_LAMPS = "lamps";
    private static final String TAG_HOMELESS = "homeless_cells";
    private static final String TAG_BUSY = "busy";
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private final VaultCellSlots cells = new VaultCellSlots(this);
    private final List<ItemStack> homeless = new ArrayList<>();
    private final DeviceUpgrades upgrades = new DeviceUpgrades(this, UPGRADE_LIMITS, this::refreshNetwork);
    private int priority;
    /** Lamps of all slots as packed by {@link VaultLamp}; on the client, as last sent. */
    private long lamps;
    /** The slots shown as working, one bit per slot; on the client, as last sent. */
    private int busy;

    public StorageVaultBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.STORAGE_VAULT.get(), pos, state, StorageVaultBlock::animationOf);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final StorageVaultBlockEntity vault) {
        if (!vault.homeless.isEmpty()) {
            vault.dropHomeless(level, pos, state.getValue(StorageVaultBlock.FACING));
        }
        if (level.getGameTime() % REFRESH_INTERVAL_TICKS == 0) {
            vault.cells.flush();
            vault.refreshLamps();
        }
        if (level.getGameTime() % BUSY_PERIOD_TICKS == 0) {
            vault.refreshBusy();
        }
    }

    private void dropHomeless(final Level world, final BlockPos pos, final Direction front) {
        for (ItemStack cell : homeless) {
            Block.popResourceFromFace(world, pos, front, cell);
        }
        homeless.clear();
        setChanged();
    }

    public Container cells() {
        return cells;
    }

    public Container upgrades() {
        return upgrades.container();
    }

    public VaultLamp lampAt(final int slot) {
        return VaultLamp.unpack(lamps, slot);
    }

    public boolean isBusyAt(final int slot) {
        return (busy & 1 << slot) != 0;
    }

    /**
     * @param newPriority clamped to the {@linkplain DevicePriority range of a priority}
     */
    public void setPriority(final int newPriority) {
        final int clamped = DevicePriority.clamp(newPriority);
        if (clamped != priority) {
            priority = clamped;
            setChanged();
            refreshNetwork();
        }
    }

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public int storagePriority() {
        return priority;
    }

    @Override
    public List<Storage> storages() {
        return cells.storages();
    }

    @Override
    public Optional<ResourceFilter> discarded() {
        return VoidUpgradeItem.discardedBy(upgrades.container());
    }

    @Override
    public void cellsChanged() {
        refreshNetwork();
    }

    @Override
    public void contentsChanged() {
        setChanged();
    }

    private void refreshNetwork() {
        final NetworkController controller = controller();
        if (controller != null) {
            controller.component(NetworkComponentTypes.STORAGE).refresh(this);
        }
    }

    private void detachFromNetwork() {
        final NetworkController controller = controller();
        if (controller != null) {
            controller.component(NetworkComponentTypes.STORAGE).detach(this);
        }
    }

    private void refreshLamps() {
        final boolean powered = isNetworkPowered();
        long shown = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            shown = VaultLamp.of(powered ? cells.statusOf(slot) : null).packInto(shown, slot);
        }
        if (shown != lamps && level != null) {
            lamps = shown;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void refreshBusy() {
        final int touched = cells.takeTouched();
        final int shown = isNetworkPowered() ? touched : 0;
        if (shown != busy && level != null) {
            busy = shown;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new StorageVaultMenu(containerId, inventory, worldPosition);
    }

    /**
     * Leaves the network before the cells drop, so nothing is stored into a cell
     * already on the ground.
     */
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        detachFromNetwork();
        if (level != null) {
            cells.flush();
            Containers.dropContents(level, pos, cells);
            homeless.forEach(cell -> Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), cell));
            homeless.clear();
            upgrades.dropAndRelease(level, pos);
        }
    }

    @Override
    public void setRemoved() {
        detachFromNetwork();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        cells.save(output);
        upgrades.save(output);
        if (!homeless.isEmpty()) {
            final ValueOutput.TypedOutputList<ItemStack> list = output.list(TAG_HOMELESS, ItemStack.CODEC);
            homeless.forEach(list::add);
        }
        output.putInt(TAG_PRIORITY, priority);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        homeless.clear();
        homeless.addAll(cells.load(input));
        upgrades.load(input);
        input.listOrEmpty(TAG_HOMELESS, ItemStack.CODEC).forEach(homeless::add);
        priority = DevicePriority.clamp(input.getIntOr(TAG_PRIORITY, priority));
        lamps = input.getLongOr(TAG_LAMPS, lamps);
        busy = input.getIntOr(TAG_BUSY, busy);
    }

    /**
     * Clients only need the lamps and the working cells; cells reach them through the menu.
     */
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(TAG_LAMPS, lamps);
        tag.putInt(TAG_BUSY, busy);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
