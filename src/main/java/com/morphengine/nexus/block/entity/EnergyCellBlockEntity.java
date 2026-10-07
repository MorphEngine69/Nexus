package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.charging.ItemCharger;
import com.morphengine.nexus.energy.ChargeMeter;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.level.EnergyContributor;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.UpgradeHolder;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * An Energy Cell: an FE buffer that joins the energy pool of its network at
 * its own priority, by default {@value #DEFAULT_PRIORITY}, above the Storage
 * Vaults at theirs, so it fills before the energy cells of the storage and is
 * drained after them. It charges the item in its charging slot from its own
 * buffer, as fast as it gives energy.
 */
public final class EnergyCellBlockEntity extends AnimatedDeviceBlockEntity
        implements UpgradeHolder, EnergyContributor, Renamable {

    public static final int DEFAULT_PRIORITY = 10;

    private static final String TAG_ENERGY = "energy";
    private static final String TAG_PRIORITY = "priority";
    private static final String TAG_SIDES = "sides";
    private static final String TAG_CHARGING = "charging";
    private static final int CHARGE_CHECK_INTERVAL_TICKS = 20;

    private final SimpleEnergyBuffer buffer;
    private final EnergyHandler handler;
    private final CellSides sides;
    private final DeviceUpgrades upgrades = new DeviceUpgrades(this);
    private final SimpleContainer chargingSlot = new SimpleContainer(1);
    private long insertedAtLastCheck;
    private long extractedAtLastCheck;
    private int priority = DEFAULT_PRIORITY;

    public EnergyCellBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.ENERGY_CELL.get(), pos, state, EnergyCellBlock::animationOf);
        final EnergyCellTier tier = tierOf(state);
        this.buffer = new SimpleEnergyBuffer(tier.capacity(), tier.maxTransfer(), tier.maxTransfer());
        this.handler = new BufferEnergyHandler(buffer, BufferEnergyHandler.Access.RECEIVE_AND_GIVE, this::setChanged);
        this.sides = new CellSides(handler,
                new BufferEnergyHandler(buffer, BufferEnergyHandler.Access.RECEIVE_ONLY, this::setChanged),
                new BufferEnergyHandler(buffer, BufferEnergyHandler.Access.GIVE_ONLY, this::setChanged));
    }

    /**
     * Once a second, shows on the block whether energy came in since the last
     * check, and marks the cell for saving if its charge changed. The network's
     * pool moves energy straight through the buffer, past the handler that
     * marks the cell on every change.
     */
    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final EnergyCellBlockEntity cell) {
        if (ItemCharger.charge(cell.chargingSlot, cell.buffer) > 0) {
            cell.setChanged();
        }
        if (level.getGameTime() % CHARGE_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        final long inserted = cell.buffer.totalInserted();
        final long extracted = cell.buffer.totalExtracted();
        cell.recordFlows(inserted, extracted);
        final boolean charging = inserted != cell.insertedAtLastCheck;
        if (charging || extracted != cell.extractedAtLastCheck) {
            cell.setChanged();
        }
        cell.insertedAtLastCheck = inserted;
        cell.extractedAtLastCheck = extracted;
        final int charge = ChargeMeter.segments(
                cell.buffer.stored(), cell.buffer.capacity(), EnergyCellBlock.SEGMENTS);
        final BlockState shown = state.setValue(EnergyCellBlock.CHARGING, charging)
                .setValue(EnergyCellBlock.CHARGE, charge);
        if (shown != state) {
            level.setBlock(pos, shown, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Counts what flowed through the buffer since the last check as the cell's own figures: energy taken in is what the
     * cell draws, energy given out, to the network or to an item, is what it supplies.
     */
    private void recordFlows(final long inserted, final long extracted) {
        if (inserted > insertedAtLastCheck) {
            energyMeter().recordDrawn(inserted - insertedAtLastCheck);
        }
        if (extracted > extractedAtLastCheck) {
            energyMeter().recordSupplied(extracted - extractedAtLastCheck);
        }
    }

    /**
     * A cell that is upgraded keeps its block entity but changes its block, and takes the size of the new tier.
     */
    @Override
    public void setBlockState(final BlockState state) {
        super.setBlockState(state);
        final EnergyCellTier tier = tierOf(state);
        buffer.resize(tier.capacity(), tier.maxTransfer(), tier.maxTransfer());
    }

    private static EnergyCellTier tierOf(final BlockState state) {
        if (state.getBlock() instanceof EnergyCellBlock cell) {
            return cell.tier();
        }
        throw new IllegalStateException("energy cell block entity on a non energy cell block: " + state);
    }

    @Override
    public EnergyBuffer energyBuffer() {
        return buffer;
    }

    @Override
    public int energyPriority() {
        return priority;
    }

    /**
     * Moves the cell to another place in the pool of its network.
     *
     * @param newPriority clamped to the {@linkplain DevicePriority range of a priority}
     */
    public void setEnergyPriority(final int newPriority) {
        final int clamped = DevicePriority.clamp(newPriority);
        if (clamped == priority) {
            return;
        }
        priority = clamped;
        setChanged();
        final NetworkController controller = controller();
        if (controller != null) {
            controller.invalidateNetwork();
        }
    }

    /**
     * @return the handler of the cell for the network: the pool of the network moves FE through the buffer, and a
     *         device of the network gets this handler through {@link #energyHandlerBeyond}
     */
    @Override
    public EnergyHandler energyHandler() {
        return handler;
    }

    /**
     * What the cell shows a block that asks for its energy from {@code side}: a block of a network always gets it, a
     * block of another mod only through a side that was opened and as its mode says, see {@link CellSides}.
     * Cached answers are dropped when a neighbour changes, see {@link EnergyCellBlock}.
     *
     * @param side the side of the cell that is asked about; {@code null} when the asker names no side
     */
    public @Nullable EnergyHandler energyHandlerBeyond(final @Nullable Direction side) {
        return level == null ? null : sides.handlerFor(level, worldPosition, side);
    }

    public SideMode sideMode(final Direction side) {
        return sides.mode(side);
    }

    /**
     * @return the modes of all six sides as one number, to send to a panel
     */
    public int sideBits() {
        return sides.bits();
    }

    /**
     * Opens, closes or changes a side, and has other blocks ask again what the cell gives them.
     */
    public void setSideMode(final Direction side, final SideMode mode) {
        if (sides.set(side, mode)) {
            setChanged();
            if (level != null) {
                level.invalidateCapabilities(worldPosition);
            }
        }
    }

    public Container upgrades() {
        return upgrades.container();
    }

    /**
     * @return the slot of the item the cell charges from its own buffer
     */
    public Container chargingSlot() {
        return chargingSlot;
    }

    /**
     * Broken, the cell drops its upgrade and lets go of its chunk.
     */
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            upgrades.dropAndRelease(level, pos);
            Containers.dropContents(level, pos, chargingSlot);
        }
    }

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new EnergyCellMenu(containerId, inventory, worldPosition);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(TAG_ENERGY, buffer.stored());
        output.putInt(TAG_PRIORITY, priority);
        output.putInt(TAG_SIDES, sides.bits());
        upgrades.save(output);
        ContainerHelper.saveAllItems(output.child(TAG_CHARGING), chargingSlot.getItems());
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        final long stored = Math.max(0, input.getLongOr(TAG_ENERGY, 0));
        buffer.restore(SimpleEnergyBuffer.Snapshot.storing(stored));
        priority = DevicePriority.clamp(input.getIntOr(TAG_PRIORITY, DEFAULT_PRIORITY));
        sides.restore(input.getIntOr(TAG_SIDES, 0));
        upgrades.load(input);
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_CHARGING), chargingSlot.getItems());
    }
}
