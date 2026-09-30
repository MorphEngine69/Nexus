package com.morphengine.nexus.block;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.block.entity.CraftingMonitorBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
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
import org.jspecify.annotations.Nullable;

/**
 * Crafting Monitor: shows the crafting tasks of its network, their progress and
 * what holds them up, and cancels them. Its screen faces the player who placed
 * it and takes no cable; it glows while the network has energy and shows a
 * busy pattern while a task runs.
 */
public final class CraftingMonitorBlock extends NetworkDeviceBlock {

    public static final MapCodec<CraftingMonitorBlock> CODEC = simpleCodec(CraftingMonitorBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** The network has energy. Set only by the server. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** The network has crafting tasks. Set only by the server. */
    public static final BooleanProperty ACTIVE = AssemblerBlock.ACTIVE;

    public CraftingMonitorBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<CraftingMonitorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED, ACTIVE);
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
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CraftingMonitorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.CRAFTING_MONITOR.get(),
                        CraftingMonitorBlockEntity::serverTick);
    }
}
