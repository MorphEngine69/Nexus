package com.morphengine.nexus.block;

import com.geckolib.animation.RawAnimation;
import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.machine.MachineTier;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A machine that works on resources with FE, of one {@link MachineKind} and one tier: Energy Furnace and the rest.
 * Its front, the face of the model, takes no cable and faces the player who placed it. The tier is the block: a tier
 * upgrade turns the block into the next one and keeps what it holds.
 */
public final class MachineBlock extends NetworkDeviceBlock implements TieredBlock, Turnable {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** What the machine looks like; set only by the server. */
    public static final EnumProperty<MachinePhase> PHASE = EnumProperty.create("phase", MachinePhase.class);

    private final MachineKind kind;
    private final MachineTier tier;

    public MachineBlock(final MachineKind kind, final MachineTier tier, final BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.tier = Objects.requireNonNull(tier, "tier must not be null");
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PHASE, MachinePhase.OFF));
    }

    public MachineKind kind() {
        return kind;
    }

    public MachineTier tier() {
        return tier;
    }

    /**
     * @return the animation of the model in {@code state}, which the renderer loops
     */
    public static RawAnimation animationOf(final BlockState state) {
        return ((MachineBlock) state.getBlock()).kind.animationOf(state.getValue(PHASE));
    }

    @Override
    public int rank() {
        return tier.rank();
    }

    @Override
    public @Nullable Block nextTier() {
        final var tiers = NexusBlocks.machineTiers(kind);
        return rank() < tiers.size() ? tiers.get(rank()).get() : null;
    }

    /**
     * A machine turned into one of a higher tier keeps its block entity, and so all that it holds.
     */
    @Override
    protected boolean shouldChangedStateKeepBlockEntity(final BlockState oldState) {
        return oldState.getBlock() instanceof MachineBlock other && other.kind == kind;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, PHASE);
    }

    @Override
    protected BlockState orientedFor(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public DeviceRole role() {
        return DeviceRole.MACHINE;
    }

    @Override
    public boolean acceptsConnection(final BlockState state, final Direction side) {
        return side != state.getValue(FACING);
    }

    @Override
    protected void neighborChanged(
            final BlockState state, final Level level, final BlockPos pos, final Block block,
            final @Nullable Orientation orientation, final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.redstone().receive(level.hasNeighborSignal(pos));
        }
    }

    /**
     * An empty bucket, or another container of a fluid, held to a machine that gives a fluid fills from its tank
     * without opening the panel.
     */
    @Override
    protected InteractionResult useItemOn(
            final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MachineBlockEntity machine) || machine.fluidHandler(null) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!NetworkAccess.permits(player, machine, Permission.OPEN)) {
            NetworkAccess.refuse(player, Permission.OPEN);
            return InteractionResult.SUCCESS;
        }
        final ResourceHandler<FluidResource> tank = machine.fluidHandler(null);
        return FluidUtil.interactWithFluidHandler(player, hand, pos, tank, null)
                ? InteractionResult.SUCCESS
                : InteractionResult.TRY_WITH_EMPTY_HAND;
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
        return new MachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.MACHINE.get(), MachineBlockEntity::serverTick);
    }
}
