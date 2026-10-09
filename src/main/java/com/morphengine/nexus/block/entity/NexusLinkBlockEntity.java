package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.config.NexusConfig;
import com.morphengine.nexus.level.AccessPoint;
import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.level.UpgradeHolder;
import com.morphengine.nexus.menu.NexusLinkMenu;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Objects;

/**
 * A Nexus Link: lets a Nexus Terminal bound to its network reach the network
 * from up to so many blocks away, and farther and farther with every Range
 * Upgrade, as the settings of the world say, in its own dimension; with a
 * Dimension Upgrade a terminal in
 * another dimension reaches it from any distance. Once a second it lights its
 * block while the network has energy.
 */
public final class NexusLinkBlockEntity extends AnimatedDeviceBlockEntity
        implements UpgradeHolder, AccessPoint, Renamable {

    public static final int MAX_RANGE_UPGRADES = 3;
    /** Range Upgrades share a slot; a Dimension Upgrade and a Chunk Loader Upgrade take one each. */
    public static final int UPGRADE_SLOTS = 3;
    public static final UpgradeLimits UPGRADE_LIMITS =
            new UpgradeLimits(Map.of(UpgradeTypes.RANGE, MAX_RANGE_UPGRADES, UpgradeTypes.DIMENSION, 1,
                    UpgradeTypes.CHUNK_LOADER, 1));

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
        return NexusConfig.linkRange(UpgradeLimits.count(upgrades, UpgradeTypes.RANGE.get()));
    }

    /**
     * @return whether a link with {@code upgrades} in its slots reaches terminals in other dimensions
     */
    public static boolean reachesOtherDimensionsWith(final Container upgrades) {
        return UpgradeLimits.count(upgrades, UpgradeTypes.DIMENSION.get()) > 0;
    }

    @Override
    public int range() {
        return rangeWith(upgrades);
    }

    @Override
    public boolean reachesOtherDimensions() {
        return reachesOtherDimensionsWith(upgrades);
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

    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ChunkAnchors.release(this);
        if (level != null) {
            Containers.dropContents(level, pos, upgrades);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueOutput output = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        output.saveItems(TAG_UPGRADES, upgrades.getItems());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueInput input = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        input.loadItems(TAG_UPGRADES, upgrades.getItems());
    }
}
