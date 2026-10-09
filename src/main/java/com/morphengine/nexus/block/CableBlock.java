package com.morphengine.nexus.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.network.Paint;
import com.morphengine.nexus.level.NetworkChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * Network cable of one dye color. It attaches to cables of the same color and to
 * any unpainted network block, on all six sides.
 */
public final class CableBlock extends PipeBlock implements NetworkBlock, SimpleWaterloggedBlock {

    public static final DyeColor DEFAULT_COLOR = DyeColor.BLUE;

    public static final MapCodec<CableBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DyeColor.CODEC.fieldOf("color").forGetter(CableBlock::color),
                    propertiesCodec())
            .apply(instance, CableBlock::new));

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** The network has energy: the colored band in the groove glows. Set only by the server. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final float THICKNESS = 5.0F;
    private static final float PIXELS_PER_BLOCK = 16.0F;
    private static final float APOTHEM = THICKNESS / 2 / PIXELS_PER_BLOCK;

    private final DyeColor color;
    private final Paint paint;

    public CableBlock(final DyeColor color, final BlockBehaviour.Properties properties) {
        super(APOTHEM, properties);
        this.color = color;
        this.paint = Paint.dye(color.getId());
        registerDefaultState(SideConnections.detached(stateDefinition.any())
                .setValue(WATERLOGGED, false).setValue(POWERED, false));
    }

    public DyeColor color() {
        return color;
    }

    @Override
    public Paint paint() {
        return paint;
    }

    @Override
    public boolean isDevice() {
        return false;
    }

    @Override
    protected MapCodec<CableBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        SideConnections.addProperties(builder);
        builder.add(WATERLOGGED, POWERED);
    }

    /**
     * Lights or darkens the cable at {@code pos}, if there is one and it shows the
     * other way. Neighbours are not notified. Server side only.
     */
    public static void showPower(final Level level, final BlockPos pos, final boolean powered) {
        if (!level.isLoaded(pos)) {
            return;
        }
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CableBlock && state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
        final BlockState state = defaultBlockState();
        return SideConnections.attachedTo(state, context.getLevel(), context.getClickedPos(),
                        (side, neighbour) -> joins(state, side, neighbour))
                .setValue(WATERLOGGED, inWater);
    }

    @Override
    protected BlockState updateShape(
            final BlockState state,
            final Direction directionToNeighbour,
            final BlockState neighbourState,
            final LevelAccessor level,
            final BlockPos pos,
            final BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        final boolean attached = joins(state, directionToNeighbour, neighbourState);
        return SideConnections.withSide(state, directionToNeighbour, attached);
    }

    @Override
    protected void onPlace(
            final BlockState state, final Level level, final BlockPos pos, final BlockState oldState,
            final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        NetworkChanges.blockPlaced(level, pos, state, oldState);
    }

    @Override
    protected void onRemove(
            final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
            final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            NetworkChanges.blockRemoved(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }
}
