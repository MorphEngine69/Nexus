package com.morphengine.nexus.block;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * The Nexus, controller of a network. Its model shows the network's status; a
 * Nexus that another one outranks in the same network flashes red and sheds
 * red sparks.
 */
public final class NexusBlock extends NetworkDeviceBlock {

    public static final MapCodec<NexusBlock> CODEC = simpleCodec(NexusBlock::new);
    public static final EnumProperty<NexusStatus> STATUS = EnumProperty.create("status", NexusStatus.class);

    private static final int CONFLICT_RGB = 0xE0302A;
    private static final float SPARK_SCALE = 1.0F;
    private static final double SPARK_SPREAD = 1.1;
    private static final DustParticleOptions CONFLICT_SPARK = new DustParticleOptions(CONFLICT_RGB, SPARK_SCALE);

    public NexusBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STATUS, NexusStatus.NO_ENERGY));
    }

    @Override
    protected MapCodec<NexusBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATUS);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new NexusBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.NEXUS.get(), NexusBlockEntity::serverTick);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (state.getValue(STATUS) != NexusStatus.CONFLICT) {
            return;
        }
        final double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * SPARK_SPREAD;
        final double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * SPARK_SPREAD;
        final double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * SPARK_SPREAD;
        level.addParticle(CONFLICT_SPARK, x, y, z, 0, 0, 0);
    }
}
