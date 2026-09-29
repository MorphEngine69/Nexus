package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.level.EnergyContributor;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkLink;
import com.morphengine.nexus.level.NetworkMember;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

public final class EnergyCellBlockEntity extends BlockEntity
        implements EnergyContributor, NetworkMember, MenuHost, Renamable {

    private static final String TAG_ENERGY = "energy";
    private static final int CHARGE_CHECK_INTERVAL_TICKS = 20;

    private final SimpleEnergyBuffer buffer;
    private final EnergyHandler handler;
    private final ClickGuard clickGuard = new ClickGuard();
    private final NetworkLink network = new NetworkLink();
    private final DeviceName name = new DeviceName();
    private long insertedAtLastCheck;
    private long extractedAtLastCheck;

    public EnergyCellBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.ENERGY_CELL.get(), pos, state);
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
        if (state.getValue(EnergyCellBlock.CHARGING) != charging) {
            level.setBlock(pos, state.setValue(EnergyCellBlock.CHARGING, charging), Block.UPDATE_CLIENTS);
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

    /**
     * @return the handler other mods use to push FE in and pull FE out, on any side
     */
    @Override
    public EnergyHandler energyHandler() {
        return handler;
    }

    @Override
    public void joinNetwork(final NetworkController joined) {
        network.join(joined);
    }

    @Override
    public void leaveNetwork(final NetworkController left) {
        network.leave(left);
    }

    /**
     * @return name and color of the cell's network; {@code null} when no Nexus is connected
     */
    public @Nullable NetworkBadge networkBadge() {
        return NetworkBadge.of(network);
    }

    @Override
    public Component getDisplayName() {
        return name.orDefault(getBlockState().getBlock().getName());
    }

    @Override
    public void rename(final String newName) {
        name.rename(newName);
        setChanged();
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new EnergyCellMenu(containerId, inventory, worldPosition);
    }

    @Override
    public void markClosed() {
        clickGuard.markClosed(level);
    }

    @Override
    public void markPlaced() {
        clickGuard.markPlaced(level);
    }

    @Override
    public boolean ignoresClick() {
        return clickGuard.ignoresClick(level);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(TAG_ENERGY, buffer.stored());
        name.save(output);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        final long stored = Math.max(0, input.getLongOr(TAG_ENERGY, 0));
        buffer.restore(SimpleEnergyBuffer.Snapshot.storing(stored));
        name.load(input);
    }

    @Override
    protected void applyImplicitComponents(final DataComponentGetter components) {
        super.applyImplicitComponents(components);
        name.applyFrom(components);
    }

    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        name.collectInto(components);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(final ValueOutput output) {
        DeviceName.removeFrom(output);
    }
}
