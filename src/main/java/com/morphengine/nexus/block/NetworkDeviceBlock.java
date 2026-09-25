package com.morphengine.nexus.block;

import com.morphengine.nexus.api.network.Paint;
import com.morphengine.nexus.block.entity.MenuHost;
import com.morphengine.nexus.block.entity.MenuHosts;
import com.morphengine.nexus.level.NetworkChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
        final BlockState oriented = orientedFor(context);
        return SideConnections.attachedTo(oriented, context.getLevel(), context.getClickedPos(),
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
            final LevelReader level,
            final ScheduledTickAccess ticks,
            final BlockPos pos,
            final Direction directionToNeighbour,
            final BlockPos neighbourPos,
            final BlockState neighbourState,
            final RandomSource random) {
        return SideConnections.withSide(state, directionToNeighbour,
                showsPortTo(state, directionToNeighbour, neighbourState));
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

    private boolean showsPortTo(final BlockState state, final Direction side, final BlockState neighbour) {
        return neighbour.getBlock() instanceof CableBlock && joins(state, side, neighbour);
    }
}
