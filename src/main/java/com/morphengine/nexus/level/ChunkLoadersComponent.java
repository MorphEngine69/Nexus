package com.morphengine.nexus.level;

import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The blocks of one network that hold a Chunk Loader Upgrade, as the last rebuild found them, so that the chunks the
 * network keeps loaded can be counted against the limit the settings of the world set. Server thread only.
 */
public final class ChunkLoadersComponent implements NetworkComponent {

    private List<BlockEntity> holders = List.of();

    @Override
    public void adopt(final List<NetworkMember> members) {
        final List<BlockEntity> found = new ArrayList<>();
        for (NetworkMember member : members) {
            if (member instanceof UpgradeHolder && member instanceof BlockEntity block) {
                found.add(block);
            }
        }
        holders = found;
    }

    /**
     * @param candidate the block that asks to keep its chunk loaded; it is not counted among those that do
     * @param nexus     the Nexus of the network, which may hold a Chunk Loader Upgrade too
     * @param limit     the most chunks the network may keep loaded; zero or less for no limit
     * @return whether the candidate may keep its chunk loaded: with no limit, in a chunk the network already keeps, or
     *         while the network keeps fewer chunks than the limit
     */
    public boolean allows(final BlockEntity candidate, final BlockEntity nexus, final int limit) {
        if (limit <= 0) {
            return true;
        }
        final Set<Long> kept = new HashSet<>();
        for (BlockEntity holder : holders) {
            addIfHolding(kept, holder, candidate);
        }
        addIfHolding(kept, nexus, candidate);
        return kept.contains(chunkOf(candidate)) || kept.size() < limit;
    }

    private static void addIfHolding(final Set<Long> kept, final BlockEntity holder, final BlockEntity candidate) {
        if (holder != candidate && !holder.isRemoved() && holder instanceof UpgradeHolder upgrades
                && UpgradeLimits.count(upgrades.upgrades(), UpgradeTypes.CHUNK_LOADER.get()) > 0) {
            kept.add(chunkOf(holder));
        }
    }

    private static long chunkOf(final BlockEntity block) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(block.getBlockPos().getX()),
                SectionPos.blockToSectionCoord(block.getBlockPos().getZ()));
    }
}
