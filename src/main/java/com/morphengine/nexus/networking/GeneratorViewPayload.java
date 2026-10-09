package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.GeneratorView;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: a new view for the generator menu with the given container id.
 */
public record GeneratorViewPayload(int containerId, GeneratorView view) implements CustomPacketPayload {

    public static final Type<GeneratorViewPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "generator_view"));

    private static final StreamCodec<RegistryFriendlyByteBuf, GeneratorView> VIEW_CODEC =
            StreamCodec.of(GeneratorViewPayload::writeView, GeneratorViewPayload::readView);

    public static final StreamCodec<RegistryFriendlyByteBuf, GeneratorViewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GeneratorViewPayload::containerId,
                    VIEW_CODEC, GeneratorViewPayload::view,
                    GeneratorViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void writeView(final RegistryFriendlyByteBuf buffer, final GeneratorView view) {
        buffer.writeVarLong(view.stored());
        buffer.writeVarLong(view.capacity());
        buffer.writeVarLong(view.production());
        buffer.writeVarInt(view.burnTicksLeft());
        buffer.writeVarInt(view.burnTicksTotal());
        NetworkBadgeCodec.write(buffer, view.network());
        buffer.writeVarInt(view.tanks().size());
        for (TankView tank : view.tanks()) {
            buffer.writeBoolean(tank.fluid() != null);
            if (tank.fluid() != null) {
                FluidKey.STREAM_CODEC.encode(buffer, tank.fluid());
            }
            buffer.writeVarLong(tank.amount());
            buffer.writeVarLong(tank.capacity());
        }
        buffer.writeVarInt(view.accepted().size());
        for (FluidKey fluid : view.accepted()) {
            FluidKey.STREAM_CODEC.encode(buffer, fluid);
        }
    }

    private static GeneratorView readView(final RegistryFriendlyByteBuf buffer) {
        final long stored = buffer.readVarLong();
        final long capacity = buffer.readVarLong();
        final long production = buffer.readVarLong();
        final int burnTicksLeft = buffer.readVarInt();
        final int burnTicksTotal = buffer.readVarInt();
        final var network = NetworkBadgeCodec.read(buffer);
        final int count = buffer.readVarInt();
        final List<TankView> tanks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            final FluidKey fluid = buffer.readBoolean() ? FluidKey.STREAM_CODEC.decode(buffer) : null;
            tanks.add(new TankView(fluid, buffer.readVarLong(), buffer.readVarLong()));
        }
        final int accepted = buffer.readVarInt();
        final List<FluidKey> fluids = new ArrayList<>(accepted);
        for (int index = 0; index < accepted; index++) {
            fluids.add(FluidKey.STREAM_CODEC.decode(buffer));
        }
        return new GeneratorView(stored, capacity, production, burnTicksLeft, burnTicksTotal, network, tanks,
                fluids);
    }
}
