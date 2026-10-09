package com.morphengine.nexus.block;

import com.geckolib.animation.RawAnimation;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import org.jspecify.annotations.Nullable;

/**
 * Assembler: keeps encoded Blueprints and runs them for the network's crafting
 * tasks. It crafts crafting recipes itself and hands processing to the block
 * its face touches, taking the outputs back from it where that side allows.
 * Placed against a block, it faces that block; the face takes no cable.
 */
public final class AssemblerBlock extends NetworkDeviceBlock implements Turnable {

    /** The face that touches the machine it works with. */
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    /** The network has energy. Set only by the server. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** It keeps at least one crafting task. Set only by the server. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ONLINE = RawAnimation.begin().thenLoop("online");
    private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("active");

    public AssemblerBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(ACTIVE, false));
    }

    /**
     * @return the animation of the model in {@code state}: still without energy, a slow swell with energy, busy
     *         while it keeps crafting tasks
     */
    public static RawAnimation animationOf(final BlockState state) {
        if (state.getValue(ACTIVE)) {
            return WORKING;
        }
        return state.getValue(POWERED) ? ONLINE : IDLE;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED, ACTIVE);
    }

    @Override
    protected BlockState orientedFor(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public DeviceRole role() {
        return DeviceRole.MACHINE;
    }

    @Override
    public boolean acceptsConnection(final BlockState state, final Direction side) {
        return side != state.getValue(FACING);
    }

    /**
     * The face takes no cable, but joins another Assembler it touches, and a machine of the network it faces:
     * Assemblers set against each other share the network whichever way they face, and a machine is part of the
     * network like any other device.
     */
    @Override
    public boolean acceptsConnection(final BlockState state, final Direction side, final BlockState neighbour) {
        return acceptsConnection(state, side) || neighbour.getBlock() instanceof AssemblerBlock
                || neighbour.getBlock() instanceof MachineBlock;
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
        return new AssemblerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.ASSEMBLER.get(), AssemblerBlockEntity::serverTick);
    }
}
