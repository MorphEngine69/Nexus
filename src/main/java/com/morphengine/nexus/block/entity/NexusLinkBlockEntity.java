package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.level.AccessPoint;
import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.menu.NexusLinkMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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
import java.util.Objects;

/**
 * A Nexus Link: lets a Nexus Terminal bound to its network reach the network
 * from up to {@value #BASE_RANGE} blocks away, and {@value #RANGE_PER_UPGRADE}
 * more for every Range Upgrade, in its own dimension. Once a second it lights
 * its block while the network has energy.
 */
public final class NexusLinkBlockEntity extends AnimatedDeviceBlockEntity implements AccessPoint, Renamable {

    /** Placeholder balance, like the Range Upgrades it takes. */
    public static final int BASE_RANGE = 32;
    public static final int RANGE_PER_UPGRADE = 32;
    public static final int MAX_RANGE_UPGRADES = 4;
    /** Range Upgrades share a slot; a Chunk Loader Upgrade takes another. */
    public static final int UPGRADE_SLOTS = 2;
    public static final UpgradeLimits UPGRADE_LIMITS =
            new UpgradeLimits(Map.of(UpgradeTypes.RANGE, MAX_RANGE_UPGRADES, UpgradeTypes.CHUNK_LOADER, 1));

    private static final int STATE_CHECK_INTERVAL_TICKS = 20;
    private static final String TAG_UPGRADES = "upgrades";

    private final UpgradeContainer upgrades = new UpgradeContainer(UPGRADE_SLOTS, UPGRADE_LIMITS,
            this::upgradesChanged);

    public NexusLinkBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.NEXUS_LINK.get(), pos, state, WirelessBlock::animationOf);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final NexusLinkBlockEntity link) {
        if (level.getGameTime() % STATE_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        final boolean active = link.isNetworkPowered();
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

    /**
     * @return how far the link reaches with {@code upgrades} in its slot
     */
    public static int rangeWith(final Container upgrades) {
        return BASE_RANGE + RANGE_PER_UPGRADE * UpgradeLimits.count(upgrades, UpgradeTypes.RANGE.get());
    }

    @Override
    public int range() {
        return rangeWith(upgrades);
    }

    @Override
    public GlobalPos position() {
        return GlobalPos.of(Objects.requireNonNull(level, "a link in a network stands in a level").dimension(),
                worldPosition);
    }

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new NexusLinkMenu(containerId, inventory, worldPosition);
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
        ContainerHelper.saveAllItems(output.child(TAG_UPGRADES), upgrades.getItems());
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_UPGRADES), upgrades.getItems());
    }
}
