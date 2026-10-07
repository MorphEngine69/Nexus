package com.morphengine.nexus.block;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * The block a fluid of the mod fills the world with; the constructor of {@link LiquidBlock} is not open to mods.
 */
public final class NexusFluidBlock extends LiquidBlock {

    public NexusFluidBlock(final FlowingFluid fluid, final BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
