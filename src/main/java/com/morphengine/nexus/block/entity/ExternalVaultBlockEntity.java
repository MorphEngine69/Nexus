package com.morphengine.nexus.block.entity;

import com.geckolib.animation.RawAnimation;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.ExternalVaultBlock;
import com.morphengine.nexus.external.ExternalVaultSettings;
import com.morphengine.nexus.item.VoidUpgradeItem;
import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.level.NeighbourCapabilities;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkMember;
import com.morphengine.nexus.level.PlayerActor;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.level.StorageHost;
import com.morphengine.nexus.level.UpgradeHolder;
import com.morphengine.nexus.menu.ExternalVaultMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.storage.ExternalStorage;
import com.morphengine.nexus.storage.ScanSchedule;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * External Vault: lends the items and fluids of the block its face touches to its network as one storage, at its
 * priority. What the player lists in its filter and the access it is set to decide which resources the network uses
 * and whether it may put any in. What the network knows of the block is a copy, read again every second at first, a
 * different moment for each vault so that many of them do not read together, a little less often for a large block and
 * much oftener while a player is at it, see {@link ScanSchedule}; what the network itself puts in or takes out is
 * counted at once.
 * A block that belongs to a network itself is never used, so that nothing is counted twice or goes round in a circle.
 * A Void Upgrade in its slots has the network destroy what it lists. Lights or darkens its cable arms with the energy
 * of the network.
 */
public final class ExternalVaultBlockEntity extends AnimatedDeviceBlockEntity
        implements UpgradeHolder, StorageHost, Renamable {

    public static final int FILTER_SLOTS = 9;
    /** A Capacity Upgrade adds this many filter slots; placeholder balance. */
    public static final int FILTER_SLOTS_PER_CAPACITY_UPGRADE = 9;
    public static final int UPGRADE_SLOTS = 4;
    public static final int MAX_SPEED_UPGRADES = 4;
    public static final UpgradeLimits UPGRADE_LIMITS = new UpgradeLimits(Map.of(
            UpgradeTypes.CAPACITY, 3, UpgradeTypes.CHUNK_LOADER, 1, UpgradeTypes.SPEED, MAX_SPEED_UPGRADES,
            UpgradeTypes.VOID, 1));
    /** Steps of a resource, an item or a bucket, that devices may move through a vault in a tick; placeholder. */
    public static final int BASE_STEPS_PER_TICK = 8;
    public static final int STEPS_PER_SPEED_UPGRADE = 8;

    private static final long NOT_PLANNED = -1;
    private static final int POWER_CHECK_INTERVAL_TICKS = 20;
    private static final RawAnimation AT_REST = RawAnimation.begin().thenLoop("idle");
    private static final Storage NOTHING = new SideStorage(null, null, null);
    private static final String TAG_SETTINGS = "settings";
    private static final String TAG_PRIORITY = "priority";

    private final NeighbourCapabilities neighbour = new NeighbourCapabilities();
    private final ExternalStorage storage = new ExternalStorage(this::target);
    private final List<Storage> lent = List.of(storage);
    private final UpgradeContainer upgrades = new UpgradeContainer(UPGRADE_SLOTS, UPGRADE_LIMITS,
            this::upgradesChanged);
    private final ExternalStorage.ChangeSink sink = this::reportChange;
    private ExternalVaultSettings settings = ExternalVaultSettings.DEFAULT;
    private int priority;
    private int stepsPerTick = BASE_STEPS_PER_TICK;
    private final ScanSchedule schedule = new ScanSchedule();
    private long nextScanTick = NOT_PLANNED;

    public ExternalVaultBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.EXTERNAL_VAULT.get(), pos, state, current -> AT_REST);
        storage.limit(resource -> NexusResources.of(resource).type().unit().step() * stepsPerTick,
                actor -> !(actor instanceof PlayerActor));
        configureStorage();
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final ExternalVaultBlockEntity vault) {
        vault.storage.startTick();
        final long time = level.getGameTime();
        if (time % POWER_CHECK_INTERVAL_TICKS == 0) {
            final boolean powered = vault.isNetworkPowered();
            if (state.getValue(ExternalVaultBlock.POWERED) != powered) {
                level.setBlock(pos, state.setValue(ExternalVaultBlock.POWERED, powered), Block.UPDATE_CLIENTS);
            }
        }
        if (vault.nextScanTick == NOT_PLANNED) {
            vault.nextScanTick = time + Math.floorMod(pos.asLong(), ScanSchedule.BASE_INTERVAL_TICKS);
        }
        if (time >= vault.nextScanTick) {
            if (vault.controller() != null) {
                vault.schedule.scanned(vault.storage.rescan(vault.sink));
            }
            vault.nextScanTick = time + vault.schedule.intervalTicks(time);
        }
    }

    /**
     * Has the block it lends read often for a while, for when a player is at it and may take things out by hand.
     */
    public void watch(final long time) {
        schedule.watch(time);
        nextScanTick = Math.min(nextScanTick, time + ScanSchedule.WATCH_INTERVAL_TICKS);
    }

    public ExternalVaultSettings settings() {
        return settings;
    }

    public Container upgrades() {
        return upgrades;
    }

    /**
     * @return filter slots the vault offers now: {@value #FILTER_SLOTS} plus
     *         {@value #FILTER_SLOTS_PER_CAPACITY_UPGRADE} for every Capacity Upgrade it holds
     */
    public static int filterSlotCount(final Container upgrades) {
        return FILTER_SLOTS + FILTER_SLOTS_PER_CAPACITY_UPGRADE
                * UpgradeLimits.count(upgrades, UpgradeTypes.CAPACITY.get());
    }

    public void changeSettings(final ExternalVaultSettings newSettings) {
        settings = newSettings;
        configureStorage();
        scan();
        setChanged();
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
        return lent;
    }

    @Override
    public Optional<ResourceFilter> discarded() {
        return VoidUpgradeItem.discardedBy(upgrades);
    }

    /**
     * @return what the face of the vault touches as one storage of its items and fluids; nothing where it touches a
     *         block of a network
     */
    private Storage target() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return NOTHING;
        }
        final Direction face = getBlockState().getValue(ExternalVaultBlock.FACING);
        if (serverLevel.getBlockEntity(worldPosition.relative(face)) instanceof NetworkMember) {
            return NOTHING;
        }
        return neighbour.itemsAndFluids(serverLevel, worldPosition, face);
    }

    private void upgradesChanged() {
        readSpeed();
        configureStorage();
        scan();
        ChunkAnchors.follow(this, upgrades);
        setChanged();
        refreshNetwork();
    }

    private void readSpeed() {
        stepsPerTick = BASE_STEPS_PER_TICK + STEPS_PER_SPEED_UPGRADE * upgrades.count(UpgradeTypes.SPEED);
    }

    private void configureStorage() {
        storage.configure(settings.filter().limitedTo(filterSlotCount(upgrades)).toResourceFilter(),
                settings.access());
    }

    /**
     * Reads the block at once, as after a change of what the vault allows.
     */
    private void scan() {
        if (controller() != null) {
            schedule.scanned(storage.rescan(sink));
        }
        nextScanTick = level != null ? level.getGameTime() + schedule.intervalTicks(level.getGameTime())
                : NOT_PLANNED;
    }

    private void reportChange(final ResourceKey resource, final long delta) {
        final NetworkController controller = controller();
        if (controller != null) {
            controller.component(NetworkComponentTypes.STORAGE).storage().sourceChanged(storage, resource, delta);
        }
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

    @Override
    public void writeMenuData(final RegistryFriendlyByteBuf buffer) {
        ExternalVaultSettings.STREAM_CODEC.encode(buffer, settings);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new ExternalVaultMenu(containerId, inventory, worldPosition, settings);
    }

    /**
     * Leaves the network before the block goes, so that nothing is put into it afterwards.
     */
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        detachFromNetwork();
        ChunkAnchors.release(this);
        if (level != null) {
            Containers.dropContents(level, pos, upgrades);
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
        output.store(TAG_SETTINGS, ExternalVaultSettings.CODEC, settings);
        output.putInt(TAG_PRIORITY, priority);
        ContainerHelper.saveAllItems(output, upgrades.getItems());
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        settings = input.read(TAG_SETTINGS, ExternalVaultSettings.CODEC).orElse(ExternalVaultSettings.DEFAULT);
        priority = DevicePriority.clamp(input.getIntOr(TAG_PRIORITY, 0));
        ContainerHelper.loadAllItems(input, upgrades.getItems());
        readSpeed();
        configureStorage();
    }
}
