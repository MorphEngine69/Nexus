package com.morphengine.nexus.block;

import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * A block that is one tier of a line of blocks that differ only in size, such as the Energy Cells. A tier upgrade
 * turns it into the next one, keeping what the block holds.
 */
public interface TieredBlock {

    /**
     * @return the place of this tier in the line, from 1
     */
    int rank();

    /**
     * @return the block of the next tier; {@code null} for the last
     */
    @Nullable Block nextTier();
}
