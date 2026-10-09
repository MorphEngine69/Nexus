package com.morphengine.nexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block entity that has something to do just before its block is broken or replaced: drop what it holds, let go of
 * what it uses.
 */
public interface RemovalEffects {

    void preRemoveSideEffects(BlockPos pos, BlockState state);
}
