package com.morphengine.nexus.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.transfer.TransferKind;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A Puller or Pusher: a head on the face that touches the block it works with,
 * and a cable arm reaching out to every network block on its other sides, lit
 * while the network has energy. It
 * stands on its own, without a cable, and joins the network once one reaches
 * it. Placed against a block, it faces that block.
 */
public final class TransferDeviceBlock extends NetworkDeviceBlock implements Turnable {

    public static final MapCodec<TransferDeviceBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    TransferKind.CODEC.fieldOf("kind").forGetter(TransferDeviceBlock::kind),
                    propertiesCodec())
            .apply(instance, TransferDeviceBlock::new));

    /** The face that touches the block the device works with; it takes no cable. */
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    /** The network has energy: the cable arms glow. Set only by the server. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final int SIDES = Direction.values().length;
    private static final int MASKS = 1 << SIDES;
    private static final VoxelShape[] SHAPES = buildShapes();

    private final TransferKind kind;

    public TransferDeviceBlock(final TransferKind kind, final BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    public TransferKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<TransferDeviceBlock> codec() {
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
        return kind.role();
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
        return SHAPES[state.getValue(FACING).ordinal() * MASKS + SideConnections.attachedMask(state)];
    }

    @Override
    protected void neighborChanged(
            final BlockState state, final Level level, final BlockPos pos, final Block block,
            final @Nullable Orientation orientation, final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TransferDeviceBlockEntity device) {
            device.receiveSignal(level.hasNeighborSignal(pos));
        }
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
        return new TransferDeviceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.TRANSFER_DEVICE.get(),
                        TransferDeviceBlockEntity::serverTick);
    }

    /**
     * Every combination of facing and attached arms, indexed by
     * {@code facing.ordinal() * 64 + attached mask}: the head with the core
     * behind it, and an arm of cable profile for each attached side.
     */
    private static VoxelShape[] buildShapes() {
        final Map<Direction, VoxelShape> heads = Shapes.rotateAll(Shapes.or(
                Block.box(3, 3, 0, 13, 13, 5), Block.box(5, 5, 5, 11, 11, 11)));
        final Map<Direction, VoxelShape> arms = Shapes.rotateAll(Block.box(5, 5, 0, 11, 11, 5));
        final VoxelShape[] shapes = new VoxelShape[SIDES * MASKS];
        for (Direction facing : Direction.values()) {
            for (int mask = 0; mask < MASKS; mask++) {
                shapes[facing.ordinal() * MASKS + mask] = withArms(heads.get(facing), arms, mask);
            }
        }
        return shapes;
    }

    private static VoxelShape withArms(final VoxelShape head, final Map<Direction, VoxelShape> arms, final int mask) {
        VoxelShape shape = head;
        for (Direction side : Direction.values()) {
            if ((mask & 1 << side.ordinal()) != 0) {
                shape = Shapes.or(shape, arms.get(side));
            }
        }
        return shape;
    }
}
