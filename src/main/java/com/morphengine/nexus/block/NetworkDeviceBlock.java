package com.morphengine.nexus.block;

import com.morphengine.nexus.api.network.Paint;
import com.morphengine.nexus.block.entity.MenuHost;
import com.morphengine.nexus.block.entity.MenuHosts;
import com.morphengine.nexus.block.entity.RemovalEffects;
import com.morphengine.nexus.level.NetworkChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Base for unpainted network devices with a block entity. Designed for
 * extension: it opens a port on every side where a cable is attached, shows the
 * color of the network it belongs to and reports placement and removal to the
 * network graph. A right click opens the menu of a block entity that is a
 * {@link MenuHost}. Subclasses that add block state properties must call
 * {@code super.createBlockStateDefinition}.
 */
public abstract class NetworkDeviceBlock extends BaseEntityBlock implements NetworkBlock {

    /**
     * Accent color of the model: the color chosen in the Nexus of the device's
     * network, or {@link NetworkColoring#UNCONNECTED} when no Nexus is reachable.
     * Set only by the server from the network, never by the player directly.
     */
    public static final EnumProperty<DyeColor> NETWORK_COLOR = EnumProperty.create("network_color", DyeColor.class);

    protected NetworkDeviceBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(SideConnections.detached(stateDefinition.any())
                .setValue(NETWORK_COLOR, NetworkColoring.UNCONNECTED));
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public final Paint paint() {
        return Paint.UNPAINTED;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        SideConnections.addProperties(builder);
        builder.add(NETWORK_COLOR);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return withPorts(orientedFor(context), context.getLevel(), context.getClickedPos());
    }

    /**
     * @param oriented the state of the block with its facing set, whose ports are not yet worked out
     * @return {@code oriented} with a port on every side where something attaches
     */
    public final BlockState withPorts(final BlockState oriented, final BlockGetter level, final BlockPos pos) {
        return SideConnections.attachedTo(oriented, level, pos,
                (side, neighbour) -> showsPortTo(oriented, side, neighbour));
    }

    /**
     * The state a newly placed block starts from, before its ports are worked out.
     * Devices with a facing turn it here, so that ports skip the right face.
     */
    protected BlockState orientedFor(final BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected BlockState updateShape(
            final BlockState state,
            final Direction directionToNeighbour,
            final BlockState neighbourState,
            final LevelAccessor level,
            final BlockPos pos,
            final BlockPos neighbourPos) {
        return SideConnections.withSide(state, directionToNeighbour,
                showsPortTo(state, directionToNeighbour, neighbourState));
    }

    @Override
    public void setPlacedBy(
            final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity placer,
            final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        MenuHosts.placed(level, pos, placer);
    }

    @Override
    protected void onPlace(
            final BlockState state, final Level level, final BlockPos pos, final BlockState oldState,
            final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        NetworkChanges.blockPlaced(level, pos, state, oldState);
    }

    /**
     * Whether this block, put where {@code oldState} stood, takes over the block entity that was there instead of
     * having it removed. A device turned into one of another tier keeps all that it holds.
     */
    protected boolean shouldChangedStateKeepBlockEntity(final BlockState oldState) {
        return false;
    }

    @Override
    protected void onRemove(
            final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
            final boolean movedByPiston) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }
        final boolean keepsEntity = newState.getBlock() instanceof NetworkDeviceBlock successor
                && successor.shouldChangedStateKeepBlockEntity(state);
        if (level instanceof ServerLevel serverLevel) {
            if (!keepsEntity && level.getBlockEntity(pos) instanceof RemovalEffects device) {
                device.preRemoveSideEffects(pos, state);
            }
            NetworkChanges.blockRemoved(serverLevel, pos);
        }
        if (!keepsEntity) {
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state, final Level level, final BlockPos pos, final Player player,
            final BlockHitResult hit) {
        return MenuHosts.open(level, pos, player);
    }

    /**
     * Whether the model shows something attached on {@code side}, where
     * {@code neighbour} stands. By default a port, and only for a cable; a
     * device that reaches out to every network block overrides it.
     */
    protected boolean showsPortTo(final BlockState state, final Direction side, final BlockState neighbour) {
        return neighbour.getBlock() instanceof CableBlock && joins(state, side, neighbour);
    }
}
