package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.api.transport.TransferQuota;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.menu.TransferDeviceMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.transfer.TransferKind;
import com.morphengine.nexus.transfer.TransferSettings;
import com.morphengine.nexus.transport.RedstoneGate;
import com.morphengine.nexus.transport.TransferTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * A Puller or Pusher. Once every {@value #OPERATION_INTERVAL_TICKS} ticks,
 * while its network has energy and its redstone mode lets it, it moves one
 * resource between the network and the block its face touches, as that block
 * offers it on that face. Once a second it lights or darkens its cable arms
 * with the network's energy. It keeps its settings and its upgrades.
 */
public final class TransferDeviceBlockEntity extends NetworkDeviceBlockEntity {

    public static final int FILTER_SLOTS = 9;
    public static final int UPGRADE_SLOTS = 4;
    static final int OPERATION_INTERVAL_TICKS = 10;
    private static final int POWER_CHECK_INTERVAL_TICKS = 20;

    private static final String TAG_SETTINGS = "settings";
    private static final String TAG_SIGNAL = "redstone_signal";
    /** One item or one bucket per operation until upgrades raise it. */
    private static final TransferQuota QUOTA =
            resource -> NexusResources.of(resource).type().unit().unitsPerWhole();

    private final TransferKind kind;
    private final SimpleContainer upgrades = new SimpleContainer(UPGRADE_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            TransferDeviceBlockEntity.this.setChanged();
        }
    };
    private final RedstoneGate gate = new RedstoneGate(RedstoneMode.IGNORED);
    private final RandomGenerator random = new SplittableRandom();
    private TransferSettings settings = TransferSettings.DEFAULT;
    private @Nullable TransferTask task;
    private @Nullable Direction watchedFace;
    private @Nullable BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> items;
    private @Nullable BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> fluids;
    private boolean signalKnown;
    private int cooldown;

    public TransferDeviceBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.TRANSFER_DEVICE.get(), pos, state);
        this.kind = kindOf(state);
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
        device.cooldown = OPERATION_INTERVAL_TICKS;
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
        task = null;
        if (gate.mode() != newSettings.redstone()) {
            gate.changeMode(newSettings.redstone());
        }
    }

    public Container upgrades() {
        return upgrades;
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
        if (!beside.isPresent()) {
            return;
        }
        final NetworkStorage network = controller.component(NetworkComponentTypes.STORAGE).storage();
        task().runOnce(kind.route(beside, network, Actor.NOBODY));
        gate.operated();
    }

    /**
     * What the block the device's face touches offers on the face touched.
     * The lookups are cached until the device turns to another face.
     */
    private SideStorage besideStorage(final ServerLevel level, final BlockPos pos, final Direction face) {
        BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> itemCache = items;
        BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> fluidCache = fluids;
        if (face != watchedFace || itemCache == null || fluidCache == null) {
            final BlockPos target = pos.relative(face);
            itemCache = BlockCapabilityCache.create(Capabilities.Item.BLOCK, level, target, face.getOpposite());
            fluidCache = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, target, face.getOpposite());
            items = itemCache;
            fluids = fluidCache;
            watchedFace = face;
        }
        return new SideStorage(itemCache.getCapability(), fluidCache.getCapability());
    }

    private TransferTask task() {
        TransferTask current = task;
        if (current == null) {
            current = kind.taskFor(settings, QUOTA, random);
            task = current;
        }
        return current;
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
    }
}
