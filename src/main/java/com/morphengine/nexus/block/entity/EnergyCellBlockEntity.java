package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.energy.ChargeMeter;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.level.EnergyContributor;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkNeighbours;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
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
 * drained after them.
 */
public final class EnergyCellBlockEntity extends AnimatedDeviceBlockEntity
        implements EnergyContributor, Renamable {

    public static final int DEFAULT_PRIORITY = 10;

    private static final String TAG_ENERGY = "energy";
    private static final String TAG_PRIORITY = "priority";
    private static final int CHARGE_CHECK_INTERVAL_TICKS = 20;

    private final SimpleEnergyBuffer buffer;
    private final EnergyHandler handler;
    private final DeviceUpgrades upgrades = new DeviceUpgrades(this);
    private long insertedAtLastCheck;
    private long extractedAtLastCheck;
    private int priority = DEFAULT_PRIORITY;

    public EnergyCellBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.ENERGY_CELL.get(), pos, state, EnergyCellBlock::animationOf);
        final EnergyCellTier tier = tierOf(state);
        this.buffer = new SimpleEnergyBuffer(tier.capacity(), tier.maxTransfer(), tier.maxTransfer());
        this.handler = new BufferEnergyHandler(buffer, BufferEnergyHandler.Access.RECEIVE_AND_GIVE, this::setChanged);
    }

    /**
     * Once a second, shows on the block whether energy came in since the last
     * check, and marks the cell for saving if its charge changed. The network's
     * pool moves energy straight through the buffer, past the handler that
     * marks the cell on every change.
     */
    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final EnergyCellBlockEntity cell) {
        if (level.getGameTime() % CHARGE_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        final long inserted = cell.buffer.totalInserted();
        final long extracted = cell.buffer.totalExtracted();
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
     * What the cell shows a block that asks for its energy from {@code side}. Only a block of a network may push FE
     * in or pull FE out; a block of another mod, such as the pipe of a mod of pipes, gets nothing, so that FE leaves
     * and enters the cell only through a Puller or a Pusher, which keep to the rules of the network. The answer
     * follows what stands there now, so {@link EnergyCellBlock} drops what blocks were told when a neighbour changes.
     *
     * @param side the side of the cell that is asked about; {@code null} when the asker names no side
     * @return the handler for a block of a network beyond {@code side}; {@code null} otherwise
     */
    public @Nullable EnergyHandler energyHandlerBeyond(final @Nullable Direction side) {
        return level != null && NetworkNeighbours.hasNetworkBlockBeyond(level, worldPosition, side) ? handler : null;
    }

    public Container upgrades() {
        return upgrades.container();
    }

    /**
     * Broken, the cell drops its upgrade and lets go of its chunk.
     */
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            upgrades.dropAndRelease(level, pos);
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
        upgrades.save(output);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        final long stored = Math.max(0, input.getLongOr(TAG_ENERGY, 0));
        buffer.restore(SimpleEnergyBuffer.Snapshot.storing(stored));
        priority = DevicePriority.clamp(input.getIntOr(TAG_PRIORITY, DEFAULT_PRIORITY));
        upgrades.load(input);
    }
}
