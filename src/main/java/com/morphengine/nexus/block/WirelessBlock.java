package com.morphengine.nexus.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.block.entity.NetworkReceiverBlockEntity;
import com.morphengine.nexus.block.entity.NetworkTransmitterBlockEntity;
import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * A device that carries a network beyond its cables: a Network Transmitter, a
 * Network Receiver or a Nexus Link, as its {@link WirelessKind} says. Its
 * antenna stands on top, so it takes cable on every other side.
 */
public final class WirelessBlock extends NetworkDeviceBlock {

    public static final MapCodec<WirelessBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    WirelessKind.CODEC.fieldOf("kind").forGetter(WirelessBlock::kind),
                    propertiesCodec())
            .apply(instance, WirelessBlock::new));

    /**
     * It does its job: a transmitter reaches its receiver, a receiver is
     * reached, a link has a network with energy. Set only by the server.
     */
    public static final BooleanProperty ACTIVE = BlockStateProperties.POWERED;

    private final WirelessKind kind;

    public WirelessBlock(final WirelessKind kind, final BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    public WirelessKind kind() {
        return kind;
    }

    /**
     * @return whether {@code state} is a Network Transmitter or Receiver
     */
    public static boolean isLinkEnd(final BlockState state) {
        return state.getBlock() instanceof WirelessBlock wireless && wireless.kind.isLinkEnd();
    }

    @Override
    public boolean acceptsConnection(final BlockState state, final Direction side) {
        return side != Direction.UP;
    }

    @Override
    protected MapCodec<WirelessBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return switch (kind) {
            case TRANSMITTER -> new NetworkTransmitterBlockEntity(pos, state);
            case RECEIVER -> new NetworkReceiverBlockEntity(pos, state);
            case LINK -> new NexusLinkBlockEntity(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return switch (kind) {
            case TRANSMITTER -> createTickerHelper(type, NexusBlockEntityTypes.NETWORK_TRANSMITTER.get(),
                    NetworkTransmitterBlockEntity::serverTick);
            case RECEIVER -> createTickerHelper(type, NexusBlockEntityTypes.NETWORK_RECEIVER.get(),
                    NetworkReceiverBlockEntity::serverTick);
            case LINK -> createTickerHelper(type, NexusBlockEntityTypes.NEXUS_LINK.get(),
                    NexusLinkBlockEntity::serverTick);
        };
    }
}
