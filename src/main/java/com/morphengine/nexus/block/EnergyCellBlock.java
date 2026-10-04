package com.morphengine.nexus.block;

import com.geckolib.animation.RawAnimation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
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
import org.jspecify.annotations.Nullable;

/**
 * Energy Cell: a battery that joins the network and adds its buffer to the
 * network's energy pool. Accepts and gives FE on every side.
 */
public final class EnergyCellBlock extends NetworkDeviceBlock {

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

    /**
     * @return the animation of the model: none, the bar is driven by the renderer from the state
     */
    public static RawAnimation animationOf(final BlockState state) {
        return IDLE;
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
