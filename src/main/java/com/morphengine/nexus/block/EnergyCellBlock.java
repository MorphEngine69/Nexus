package com.morphengine.nexus.block;

import com.geckolib.animation.RawAnimation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * Energy Cell: a battery that joins the network and adds its buffer to the
 * network's energy pool. Only blocks of a network reach its energy directly;
 * anything else goes through a Puller or a Pusher.
 */
public final class EnergyCellBlock extends NetworkDeviceBlock implements TieredBlock {

    public static final MapCodec<EnergyCellBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EnergyCellTier.CODEC.fieldOf("tier").forGetter(EnergyCellBlock::tier),
                    propertiesCodec())
            .apply(instance, EnergyCellBlock::new));

    /** Energy came in during the last second: the charge bars run. Set only by the server. */
    public static final BooleanProperty CHARGING = BooleanProperty.create("charging");
    /** How many of the {@link #SEGMENTS} segments of the bar the charge lights. Set only by the server. */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, EnergyCellBlock.SEGMENTS);
    public static final int SEGMENTS = 8;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private final EnergyCellTier tier;

    public EnergyCellBlock(final EnergyCellTier tier, final BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(CHARGING, false).setValue(CHARGE, 0));
    }

    public EnergyCellTier tier() {
        return tier;
    }

    @Override
    public int rank() {
        return tier.rank();
    }

    @Override
    public @Nullable Block nextTier() {
        return rank() < NexusBlocks.ENERGY_CELLS.size() ? NexusBlocks.ENERGY_CELLS.get(rank()).get() : null;
    }

    /**
     * A cell turned into one of another tier keeps its block entity, and so all that it holds.
     */
    @Override
    protected boolean shouldChangedStateKeepBlockEntity(final BlockState oldState) {
        return oldState.getBlock() instanceof EnergyCellBlock;
    }

    /**
     * @return the animation of the model: none, the bar is driven by the renderer from the state
     */
    public static RawAnimation animationOf(final BlockState state) {
        return IDLE;
    }

    /**
     * Who may reach the energy of the cell depends on what stands against it, so what other blocks have cached of
     * the cell is dropped whenever a neighbour changes.
     */
    @Override
    protected void neighborChanged(
            final BlockState state, final Level level, final BlockPos pos, final Block block,
            final @Nullable Orientation orientation, final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()) {
            level.invalidateCapabilities(pos);
        }
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected MapCodec<EnergyCellBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGING, CHARGE);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new EnergyCellBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, NexusBlockEntityTypes.ENERGY_CELL.get(), EnergyCellBlockEntity::serverTick);
    }
}
