package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Objects;

/**
 * The upgrade slots of the Nexus, the Storage Vault and the Energy Cell. They take a
 * Chunk Loader Upgrade, which keeps the chunk of the owner loaded while it sits in a
 * slot, and an owner may take more kinds by giving its own limits. The owner passes
 * on its removal, saving and loading, and may ask to be told of every change.
 */
public final class DeviceUpgrades {

    /** Slots of an owner that takes several kinds of upgrade, such as the Storage Vault. */
    public static final int SIZE = 4;
    /** Slots of an owner that takes a Chunk Loader Upgrade only. */
    public static final int SINGLE_SLOT = 1;
    public static final UpgradeLimits LIMITS = new UpgradeLimits(Map.of(UpgradeTypes.CHUNK_LOADER, 1));

    private static final String TAG_UPGRADES = "upgrades";

    private final BlockEntity owner;
    private final UpgradeContainer upgrades;
    private final Runnable onChange;

    /**
     * The one slot of an owner that takes only a Chunk Loader Upgrade, such as the Nexus and the Energy Cell.
     */
    public DeviceUpgrades(final BlockEntity owner) {
        this(owner, LIMITS, SINGLE_SLOT, () -> { });
    }

    /**
     * @param limits   the kinds the owner takes, and how many of each
     * @param onChange run after every change of the slots, once the chunk and the owner are seen to
     */
    public DeviceUpgrades(final BlockEntity owner, final UpgradeLimits limits, final Runnable onChange) {
        this(owner, limits, SIZE, onChange);
    }

    private DeviceUpgrades(
            final BlockEntity owner, final UpgradeLimits limits, final int slots, final Runnable onChange) {
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.onChange = Objects.requireNonNull(onChange, "onChange must not be null");
        this.upgrades = new UpgradeContainer(slots, Objects.requireNonNull(limits, "limits must not be null"),
                this::changed);
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
        output.saveItems(TAG_UPGRADES, upgrades.getItems());
    }

    public void load(final ValueInput input) {
        input.loadItems(TAG_UPGRADES, upgrades.getItems());
    }

    private void changed() {
        ChunkAnchors.follow(owner, upgrades);
        owner.setChanged();
        onChange.run();
    }
}
