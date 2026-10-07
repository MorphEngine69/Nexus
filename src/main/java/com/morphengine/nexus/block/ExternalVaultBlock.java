package com.morphengine.nexus.block;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * External Vault: a head on the face that touches a chest, a drawer or any other block that holds items or fluids,
 * which the network then uses as storage of its own, and a cable arm reaching out to every network block on its
 * other sides, lit while the network has energy. It stands on its own, without a cable, and joins the network once
 * one reaches it. Placed against a block, it faces that block.
 */
public final class ExternalVaultBlock extends NetworkDeviceBlock implements Turnable {

    public static final MapCodec<ExternalVaultBlock> CODEC = simpleCodec(ExternalVaultBlock::new);

    /** The face that touches the block the vault works with; it takes no cable. */
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    /** The network has energy: the cable arms glow. Set only by the server. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ExternalVaultBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<ExternalVaultBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED);
    }

    @Override
    protected BlockState orientedFor(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public DeviceRole role() {
        return DeviceRole.STORAGE;
    }

    @Override
    public boolean acceptsConnection(final BlockState state, final Direction side) {
        return side != state.getValue(FACING);
    }

    @Override
    protected boolean showsPortTo(final BlockState state, final Direction side, final BlockState neighbour) {
        return joins(state, side, neighbour);
    }

    @Override
    protected VoxelShape getShape(
            final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return HeadShapes.of(state.getValue(FACING), SideConnections.attachedMask(state));
    }

    @Override
    public EnumProperty<Direction> facingProperty() {
        return FACING;
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
        return new ExternalVaultBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.EXTERNAL_VAULT.get(),
                        ExternalVaultBlockEntity::serverTick);
    }
}
