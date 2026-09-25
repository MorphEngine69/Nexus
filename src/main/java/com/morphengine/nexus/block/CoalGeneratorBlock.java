package com.morphengine.nexus.block;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.block.entity.CoalGeneratorBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Coal Generator: a network device that burns coal, charcoal or coal blocks into
 * RF for its network and for neighbouring blocks. Its front, the firebox, faces
 * the player who placed it, glows while it produces and takes no cable.
 */
public final class CoalGeneratorBlock extends NetworkDeviceBlock {

    public static final MapCodec<CoalGeneratorBlock> CODEC = simpleCodec(CoalGeneratorBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final double CRACKLE_CHANCE = 0.1;
    private static final double SPARK_CHANCE = 0.3;
    private static final double FLUE_SMOKE_CHANCE = 0.5;
    /** Just in front of the firebox, which sits a pixel behind the front face. */
    private static final double MOUTH_REACH = 0.52;
    private static final double MOUTH_HALF_WIDTH = 0.25;
    private static final double MOUTH_BOTTOM = 3.0 / 16;
    private static final double MOUTH_HEIGHT = 6.0 / 16;
    private static final double FLUE_HALF_WIDTH = 0.15;
    private static final double SMOKE_RISE = 0.02;

    public CoalGeneratorBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<CoalGeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, LIT);
    }

    @Override
    protected BlockState orientedFor(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean acceptsConnection(final BlockState state, final Direction side) {
        return side != state.getValue(FACING);
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        final Vec3 centre = Vec3.atCenterOf(pos);
        if (random.nextDouble() < CRACKLE_CHANCE) {
            level.playLocalSound(centre.x, pos.getY(), centre.z,
                    SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        final Direction front = state.getValue(FACING);
        final Direction across = front.getClockWise();
        final double sideways = spread(random, MOUTH_HALF_WIDTH);
        final double x = centre.x + front.getStepX() * MOUTH_REACH + across.getStepX() * sideways;
        final double y = pos.getY() + MOUTH_BOTTOM + random.nextDouble() * MOUTH_HEIGHT;
        final double z = centre.z + front.getStepZ() * MOUTH_REACH + across.getStepZ() * sideways;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        if (random.nextDouble() < SPARK_CHANCE) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0, 0);
        }
        if (!state.getValue(PipeBlock.UP) && random.nextDouble() < FLUE_SMOKE_CHANCE) {
            level.addParticle(ParticleTypes.SMOKE, centre.x + spread(random, FLUE_HALF_WIDTH), pos.getY() + 1,
                    centre.z + spread(random, FLUE_HALF_WIDTH), 0, SMOKE_RISE, 0);
        }
    }

    private static double spread(final RandomSource random, final double halfWidth) {
        return (random.nextDouble() * 2 - 1) * halfWidth;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CoalGeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.COAL_GENERATOR.get(),
                        CoalGeneratorBlockEntity::serverTick);
    }
}
