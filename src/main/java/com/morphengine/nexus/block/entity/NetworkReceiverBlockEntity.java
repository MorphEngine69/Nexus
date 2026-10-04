package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.level.NetworkChanges;
import com.morphengine.nexus.level.WirelessLinks;
import com.morphengine.nexus.menu.NetworkReceiverMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
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

import java.util.Map;

/**
 * A Network Receiver: where the network of a Network Transmitter linked to it
 * comes out. The link is the transmitter's, so the receiver keeps only its
 * name and its upgrade slot, which takes a Chunk Loader Upgrade to keep its
 * chunk loaded, as the link needs. Once a second it shows on its block
 * whether a network with energy reaches it.
 */
public final class NetworkReceiverBlockEntity extends AnimatedDeviceBlockEntity implements Renamable {

    public static final int UPGRADE_SLOTS = 1;
    public static final UpgradeLimits UPGRADE_LIMITS = new UpgradeLimits(Map.of(UpgradeTypes.CHUNK_LOADER, 1));

    private static final int STATE_CHECK_INTERVAL_TICKS = 20;
    private static final String TAG_UPGRADES = "upgrades";

    private final UpgradeContainer upgrades = new UpgradeContainer(UPGRADE_SLOTS, UPGRADE_LIMITS,
            this::upgradesChanged);

    public NetworkReceiverBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.NETWORK_RECEIVER.get(), pos, state, WirelessBlock::animationOf);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final NetworkReceiverBlockEntity receiver) {
        if (level.getGameTime() % STATE_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        final boolean active = receiver.isNetworkPowered();
        if (state.getValue(WirelessBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(WirelessBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    public Container upgrades() {
        return upgrades;
    }

    private void upgradesChanged() {
        ChunkAnchors.follow(this, upgrades);
        setChanged();
    }

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new NetworkReceiverMenu(containerId, inventory, worldPosition);
    }

    /**
     * Broken, the receiver drops its upgrade, lets go of its chunk and tells the
     * transmitters linked to it, so their networks let go of what stood on its
     * side.
     */
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ChunkAnchors.release(this);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Containers.dropContents(serverLevel, pos, upgrades);
        final MinecraftServer server = serverLevel.getServer();
        for (GlobalPos transmitter : WirelessLinks.of(server).partnersOf(GlobalPos.of(serverLevel.dimension(), pos))) {
            final ServerLevel at = server.getLevel(transmitter.dimension());
            if (at != null && at.isLoaded(transmitter.pos())) {
                NetworkChanges.linkChanged(at, transmitter.pos());
            }
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output.child(TAG_UPGRADES), upgrades.getItems());
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_UPGRADES), upgrades.getItems());
    }
}
