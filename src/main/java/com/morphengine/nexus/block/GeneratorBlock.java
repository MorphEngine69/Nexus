package com.morphengine.nexus.block;

import com.geckolib.animation.RawAnimation;
import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.generator.GeneratorEffects;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A generator of one {@link GeneratorKind}: a network device that burns coal, nether stars, lava, steam or biofuel
 * into FE for its network and for neighbouring blocks. Its front, the face of the model, faces the player who placed
 * it and takes no cable. A bucket used on a generator that burns fluids pours into its tank.
 */
public final class GeneratorBlock extends NetworkDeviceBlock implements Turnable {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** What the generator looks like; set only by the server. */
    public static final EnumProperty<MachinePhase> PHASE = EnumProperty.create("phase", MachinePhase.class);

    private final GeneratorKind kind;

    public GeneratorBlock(final GeneratorKind kind, final BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PHASE, MachinePhase.OFF));
    }

    public GeneratorKind kind() {
        return kind;
    }

    /**
     * @return the animation of the model in {@code state}, which the renderer loops
     */
    public static RawAnimation animationOf(final BlockState state) {
        return ((GeneratorBlock) state.getBlock()).kind.animationOf(state.getValue(PHASE));
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
    public boolean acceptsConnection(final BlockState state, final Direction side) {
        return side != state.getValue(FACING);
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

    /**
     * A bucket, or another container of a fluid, held to a generator that burns fluids pours into its tank without
     * opening the panel; the empty container stays in the hand.
     */
    @Override
    protected InteractionResult useItemOn(
            final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (!kind.burnsFluid() || !(level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!NetworkAccess.permits(player, generator, Permission.OPEN)) {
            NetworkAccess.refuse(player, Permission.OPEN);
            return InteractionResult.SUCCESS;
        }
        return pour(player, hand, pos, generator) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static boolean pour(
            final Player player, final InteractionHand hand, final BlockPos pos, final GeneratorBlockEntity generator) {
        final ResourceHandler<FluidResource> tanks = generator.fluidHandler(null);
        return tanks != null && FluidUtil.interactWithFluidHandler(player, hand, pos, tanks, null);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        GeneratorEffects.animate(kind, state.getValue(PHASE), state.getValue(FACING), level, pos, random);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.GENERATOR.get(),
                        GeneratorBlockEntity::serverTick);
    }
}
