package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.CoalGeneratorView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: a new view for the Coal Generator menu with the given container id.
 */
public record CoalGeneratorViewPayload(int containerId, CoalGeneratorView view) implements CustomPacketPayload {

    public static final Type<CoalGeneratorViewPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "coal_generator_view"));

    private static final StreamCodec<RegistryFriendlyByteBuf, CoalGeneratorView> VIEW_CODEC =
            StreamCodec.of(CoalGeneratorViewPayload::writeView, CoalGeneratorViewPayload::readView);

    public static final StreamCodec<RegistryFriendlyByteBuf, CoalGeneratorViewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CoalGeneratorViewPayload::containerId,
                    VIEW_CODEC, CoalGeneratorViewPayload::view,
                    CoalGeneratorViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void writeView(final RegistryFriendlyByteBuf buffer, final CoalGeneratorView view) {
        buffer.writeVarLong(view.stored());
        buffer.writeVarLong(view.capacity());
        buffer.writeVarLong(view.production());
        buffer.writeVarInt(view.burnTicksLeft());
        buffer.writeVarInt(view.burnTicksTotal());
        NetworkBadgeCodec.write(buffer, view.network());
    }

    private static CoalGeneratorView readView(final RegistryFriendlyByteBuf buffer) {
        final long stored = buffer.readVarLong();
        final long capacity = buffer.readVarLong();
        final long production = buffer.readVarLong();
        final int burnTicksLeft = buffer.readVarInt();
        final int burnTicksTotal = buffer.readVarInt();
        return new CoalGeneratorView(stored, capacity, production, burnTicksLeft, burnTicksTotal,
                NetworkBadgeCodec.read(buffer));
    }
}
