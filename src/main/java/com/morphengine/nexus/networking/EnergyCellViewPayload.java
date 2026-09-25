package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.EnergyCellView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: a new view for the Energy Cell menu with the given container id.
 */
public record EnergyCellViewPayload(int containerId, EnergyCellView view) implements CustomPacketPayload {

    public static final Type<EnergyCellViewPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "energy_cell_view"));

    private static final StreamCodec<RegistryFriendlyByteBuf, EnergyCellView> VIEW_CODEC =
            StreamCodec.of(EnergyCellViewPayload::writeView, EnergyCellViewPayload::readView);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyCellViewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, EnergyCellViewPayload::containerId,
                    VIEW_CODEC, EnergyCellViewPayload::view,
                    EnergyCellViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void writeView(final RegistryFriendlyByteBuf buffer, final EnergyCellView view) {
        buffer.writeVarLong(view.stored());
        buffer.writeVarLong(view.capacity());
        buffer.writeVarLong(view.input());
        buffer.writeVarLong(view.output());
        NetworkBadgeCodec.write(buffer, view.network());
    }

    private static EnergyCellView readView(final RegistryFriendlyByteBuf buffer) {
        final long stored = buffer.readVarLong();
        final long capacity = buffer.readVarLong();
        final long input = buffer.readVarLong();
        final long output = buffer.readVarLong();
        return new EnergyCellView(stored, capacity, input, output, NetworkBadgeCodec.read(buffer));
    }
}
