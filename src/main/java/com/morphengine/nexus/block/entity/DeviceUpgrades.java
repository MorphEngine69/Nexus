package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Map;
import java.util.Objects;

/**
 * The upgrade slots shared by the Nexus, the Storage Vault and the Energy Cell:
 * for now they take a Chunk Loader Upgrade, which keeps the chunk of the owner
 * loaded while it sits in a slot. The owner passes on its removal, saving and
 * loading.
 */
public final class DeviceUpgrades {

    public static final int SIZE = 4;
    public static final UpgradeLimits LIMITS = new UpgradeLimits(Map.of(UpgradeTypes.CHUNK_LOADER, 1));

    private static final String TAG_UPGRADES = "upgrades";

    private final BlockEntity owner;
    private final UpgradeContainer upgrades;

    public DeviceUpgrades(final BlockEntity owner) {
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.upgrades = new UpgradeContainer(SIZE, LIMITS, this::changed);
    }

    public Container container() {
        return upgrades;
    }

    /**
     * Lets go of the chunk and drops what the slots hold, for when the owner is broken.
     */
    public void dropAndRelease(final Level level, final BlockPos pos) {
        ChunkAnchors.release(owner);
        Containers.dropContents(level, pos, upgrades);
    }

    public void save(final ValueOutput output) {
        ContainerHelper.saveAllItems(output.child(TAG_UPGRADES), upgrades.getItems());
    }

    public void load(final ValueInput input) {
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_UPGRADES), upgrades.getItems());
    }

    private void changed() {
        ChunkAnchors.follow(owner, upgrades);
        owner.setChanged();
    }
}
