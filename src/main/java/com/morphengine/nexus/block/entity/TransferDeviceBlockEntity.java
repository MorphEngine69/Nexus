package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.api.transport.TransferQuota;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.level.NeighbourCapabilities;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.menu.TransferDeviceMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.storage.SingleTypeStorage;
import com.morphengine.nexus.transfer.DeliveryMode;
import com.morphengine.nexus.transfer.DeviceOperation;
import com.morphengine.nexus.transfer.TransferKind;
import com.morphengine.nexus.transfer.TransferSettings;
import com.morphengine.nexus.transfer.Workplace;
import com.morphengine.nexus.transport.RedstoneGate;
import com.morphengine.nexus.transport.Shortfalls;
import com.morphengine.nexus.transport.StockEntry;
import com.morphengine.nexus.transport.TransferRate;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import com.morphengine.nexus.world.FrontSpace;
import com.morphengine.nexus.world.HarvestTool;
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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * A Puller, Pusher, Placer or Remover. Once every few ticks, while its network
 * has energy and its redstone mode lets it, it runs one operation of its
 * {@link TransferKind}: moves one resource between the network and the block
 * its face touches, as that block offers it on that face, or places, drops,
 * breaks or picks up in the space its face touches. It moves only the kind of
 * resource it is set to. Speed Upgrades make it work more often, a Stack
 * Upgrade move more at once, a Regulator Upgrade lets it keep stock, Capacity
 * Upgrades widen its filter, and Fortune and Silk Touch Upgrades go on the
 * tool a Remover breaks with; see {@link TransferRate}. Once a second it
 * lights or darkens its cable arms with the network's energy. It keeps its
 * settings and its upgrades.
 */
public final class TransferDeviceBlockEntity extends NetworkDeviceBlockEntity {

    public static final int FILTER_SLOTS = 9;
    /** A Capacity Upgrade adds this many filter slots; placeholder balance. */
    public static final int FILTER_SLOTS_PER_CAPACITY_UPGRADE = 9;
    /** Upgrade slots; which upgrades go in them is up to the {@linkplain TransferKind#upgradeLimits kind}. */
    public static final int UPGRADE_SLOTS = 4;
    /** A Pusher with an Autocrafting Upgrade orders a craft at most once per so many operations. */
    private static final int OPERATIONS_PER_ORDER = 10;
    private static final int POWER_CHECK_INTERVAL_TICKS = 20;

    private static final String TAG_SETTINGS = "settings";
    private static final String TAG_SIGNAL = "redstone_signal";
    private final TransferKind kind;
    private final UpgradeContainer upgrades;
    private TransferRate rate = TransferRate.BASE;
    private HarvestTool tool = HarvestTool.PLAIN;
    private boolean regulated;
    private boolean autocrafts;
    private int operationsUntilOrder;
    /** One step of the resource per operation, times what a Stack Upgrade adds. */
    private final TransferQuota quota =
            resource -> NexusResources.of(resource).type().unit().step() * rate.multiplier();
    private final RedstoneGate gate = new RedstoneGate(RedstoneMode.IGNORED);
    private final RandomGenerator random = new SplittableRandom();
    private final NeighbourCapabilities neighbour = new NeighbourCapabilities();
    private TransferSettings settings = TransferSettings.DEFAULT;
    private @Nullable DeviceOperation operation;
    private boolean signalKnown;
    private int cooldown;

    public TransferDeviceBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.TRANSFER_DEVICE.get(), pos, state);
        this.kind = kindOf(state);
        this.upgrades = new UpgradeContainer(UPGRADE_SLOTS, kind.upgradeLimits(), this::upgradesChanged);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final TransferDeviceBlockEntity device) {
        if (!device.signalKnown) {
            device.gate.restore(level.hasNeighborSignal(pos));
            device.signalKnown = true;
        }
        if (level.getGameTime() % POWER_CHECK_INTERVAL_TICKS == 0) {
            device.showPower(level, pos, state);
        }
        if (--device.cooldown > 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        device.cooldown = device.rate.intervalTicks();
        device.operate(serverLevel, pos, state.getValue(TransferDeviceBlock.FACING));
    }

    private void showPower(final Level level, final BlockPos pos, final BlockState state) {
        final boolean powered = isNetworkPowered();
        if (state.getValue(TransferDeviceBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(TransferDeviceBlock.POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    private static TransferKind kindOf(final BlockState state) {
        if (state.getBlock() instanceof TransferDeviceBlock device) {
            return device.kind();
        }
        throw new IllegalStateException("transfer device block entity on another block: " + state);
    }

    public TransferKind kind() {
        return kind;
    }

    public TransferSettings settings() {
        return settings;
    }

    public void changeSettings(final TransferSettings newSettings) {
        applySettings(newSettings);
        setChanged();
    }

    private void applySettings(final TransferSettings newSettings) {
        settings = newSettings;
        operation = null;
        if (gate.mode() != newSettings.redstone()) {
            gate.changeMode(newSettings.redstone());
        }
    }

    public Container upgrades() {
        return upgrades;
    }

    /**
     * @return filter slots the device offers now: {@value #FILTER_SLOTS} plus
     *         {@value #FILTER_SLOTS_PER_CAPACITY_UPGRADE} for every Capacity
     *         Upgrade it holds
     */
    public static int filterSlotCount(final Container upgrades) {
        final int capacityUpgrades = UpgradeLimits.count(upgrades, UpgradeTypes.CAPACITY.get());
        return FILTER_SLOTS + FILTER_SLOTS_PER_CAPACITY_UPGRADE * capacityUpgrades;
    }

    private void upgradesChanged() {
        readUpgrades();
        ChunkAnchors.follow(this, upgrades);
        setChanged();
    }

    /**
     * Takes in the upgrades held now: how often and how much the device works,
     * whether it may keep stock and order crafts, and the tool it breaks with.
     */
    private void readUpgrades() {
        rate = TransferRate.of(upgrades.count(UpgradeTypes.SPEED), upgrades.count(UpgradeTypes.STACK));
        regulated = upgrades.count(UpgradeTypes.REGULATOR) > 0;
        autocrafts = upgrades.count(UpgradeTypes.AUTOCRAFTING) > 0;
        tool = new HarvestTool(upgrades.count(UpgradeTypes.FORTUNE), upgrades.count(UpgradeTypes.SILK_TOUCH) > 0);
        operation = null;
    }

    /**
     * Takes the redstone signal the device receives now. Server side only.
     */
    public void receiveSignal(final boolean signal) {
        final boolean changed = gate.isPowered() != signal;
        gate.receive(signal);
        signalKnown = true;
        if (changed) {
            setChanged();
        }
    }

    private void operate(final ServerLevel level, final BlockPos pos, final Direction face) {
        final NetworkController controller = controller();
        if (controller == null || !gate.isOpen() || !isNetworkPowered()) {
            return;
        }
        final SideStorage beside = besideStorage(level, pos, face);
        final SingleTypeStorage network =
                new SingleTypeStorage(controller.resources(), settings.resource().resourceType());
        operation().run(new Workplace(network, beside, new FrontSpace(level, pos, face, tool)));
        gate.operated();
        if (autocrafts && kind.ordersCrafts() && --operationsUntilOrder <= 0) {
            operationsUntilOrder = OPERATIONS_PER_ORDER;
            orderCraft(controller, beside);
        }
    }

    /**
     * Orders a craft of the first resource the whitelist lists that the
     * network lacks and can craft, unless one is being crafted already.
     *
     * @param beside where the device delivers to, for the amounts it keeps there
     */
    private void orderCraft(final NetworkController controller, final SideStorage beside) {
        final TransferSettings effective = effectiveSettings();
        final AutocraftingComponent autocrafting = controller.component(NetworkComponentTypes.AUTOCRAFTING);
        if (effective.filter().mode() != FilterMode.ALLOW) {
            return;
        }
        final List<StockEntry> orderable = new ArrayList<>();
        for (StockEntry entry : effective.stock()) {
            if (!autocrafting.isCrafting(entry.resource())
                    && !autocrafting.blueprints().blueprintsFor(entry.resource()).isEmpty()) {
                orderable.add(entry);
            }
        }
        final NetworkStorage storage = controller.component(NetworkComponentTypes.STORAGE).storage();
        final ResourceAmount lacking = Shortfalls.first(orderable, beside, storage, quota);
        if (lacking != null) {
            autocrafting.start(autocrafting.plan(lacking.resource(), lacking.amount(), storage),
                    getDisplayName().getString());
        }
    }

    /**
     * What the block the device's face touches offers on the face touched, of
     * the resource the device moves.
     */
    private SideStorage besideStorage(final ServerLevel level, final BlockPos pos, final Direction face) {
        return switch (settings.resource()) {
            case ITEM -> neighbour.items(level, pos, face);
            case FLUID -> neighbour.fluids(level, pos, face);
            case ENERGY -> neighbour.energy(level, pos, face);
        };
    }

    /**
     * What the device does as set, except that without a Regulator Upgrade it
     * does not keep stock, and its filter only reaches the slots its Capacity
     * Upgrades unlock now: entries left over from a removed one stay saved but
     * take no part until it is added back.
     */
    private DeviceOperation operation() {
        DeviceOperation current = operation;
        if (current == null) {
            current = kind.operationFor(effectiveSettings(), quota, random);
            operation = current;
        }
        return current;
    }

    private TransferSettings effectiveSettings() {
        final TransferSettings limited = settings.withFilter(settings.filter().limitedTo(filterSlotCount(upgrades)));
        return regulated ? limited : limited.withDelivery(DeliveryMode.UNLIMITED);
    }

    @Override
    public void writeMenuData(final RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(kind);
        TransferSettings.STREAM_CODEC.encode(buffer, settings);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new TransferDeviceMenu(containerId, inventory, worldPosition, kind, settings);
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ChunkAnchors.release(this);
        if (level != null) {
            Containers.dropContents(level, pos, upgrades);
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.store(TAG_SETTINGS, TransferSettings.CODEC, settings);
        output.putBoolean(TAG_SIGNAL, gate.isPowered());
        ContainerHelper.saveAllItems(output, upgrades.getItems());
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        applySettings(input.read(TAG_SETTINGS, TransferSettings.CODEC).orElse(TransferSettings.DEFAULT));
        gate.restore(input.getBooleanOr(TAG_SIGNAL, false));
        signalKnown = true;
        ContainerHelper.loadAllItems(input, upgrades.getItems());
        readUpgrades();
    }
}
