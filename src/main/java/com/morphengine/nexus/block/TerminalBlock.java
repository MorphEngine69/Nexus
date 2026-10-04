package com.morphengine.nexus.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.network.Paint;
import com.morphengine.nexus.block.entity.MenuHost;
import com.morphengine.nexus.block.entity.MenuHosts;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.level.NetworkChanges;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.terminal.TerminalKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A terminal: a thin panel that sits on a cable, its back against the cable
 * and its screen facing away. It joins the network only through its back, is
 * placed only against a cable and falls off when that cable is gone. Its
 * screen glows while the network has energy.
 */
public final class TerminalBlock extends BaseEntityBlock implements NetworkBlock {

    public static final MapCodec<TerminalBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    TerminalKind.CODEC.fieldOf("kind").forGetter(TerminalBlock::kind),
                    propertiesCodec())
            .apply(instance, TerminalBlock::new));

    /** The direction the screen faces; the cable is on the opposite side. */
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    /** The network has energy: the screen glows. Set only by the server. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.box(3, 3, 14, 13, 13, 16));

    private final TerminalKind kind;

    public TerminalBlock(final TerminalKind kind, final BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(NetworkDeviceBlock.NETWORK_COLOR, NetworkColoring.UNCONNECTED).setValue(POWERED, false));
    }

    public TerminalKind kind() {
        return kind;
    }

    @Override
    public Paint paint() {
        return Paint.UNPAINTED;
    }

    @Override
    public boolean acceptsConnection(final BlockState state, final Direction side) {
        return side == state.getValue(FACING).getOpposite();
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected MapCodec<TerminalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NetworkDeviceBlock.NETWORK_COLOR, POWERED);
    }

    @Override
    protected VoxelShape getShape(
            final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final Direction back = state.getValue(FACING).getOpposite();
        return level.getBlockState(pos.relative(back)).getBlock() instanceof CableBlock;
    }

    @Override
    protected BlockState updateShape(
            final BlockState state,
            final LevelReader level,
            final ScheduledTickAccess ticks,
            final BlockPos pos,
            final Direction directionToNeighbour,
            final BlockPos neighbourPos,
            final BlockState neighbourState,
            final RandomSource random) {
        final boolean supportChanged = directionToNeighbour == state.getValue(FACING).getOpposite();
        return supportChanged && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public void setPlacedBy(
            final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity placer,
            final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof MenuHost host) {
            host.markPlaced();
        }
    }

    @Override
    protected void onPlace(
            final BlockState state, final Level level, final BlockPos pos, final BlockState oldState,
            final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        NetworkChanges.blockPlaced(level, pos, state, oldState);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        NetworkChanges.blockRemoved(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state, final Level level, final BlockPos pos, final Player player,
            final BlockHitResult hit) {
        return MenuHosts.open(level, pos, player);
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
        return new TerminalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.TERMINAL.get(), TerminalBlockEntity::serverTick);
    }
}
