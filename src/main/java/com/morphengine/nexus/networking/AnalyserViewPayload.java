package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.analysis.AnalyserLine;
import com.morphengine.nexus.analysis.AnalyserLines;
import com.morphengine.nexus.api.network.DeviceEnergyUse;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.menu.AnalyserView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server to client: a new view for the Analyser menu with the given container id.
 */
public record AnalyserViewPayload(int containerId, AnalyserView view) implements CustomPacketPayload {

    public static final Type<AnalyserViewPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "analyser_view"));

    private static final StreamCodec<RegistryFriendlyByteBuf, AnalyserView> VIEW_CODEC =
            StreamCodec.of(AnalyserViewPayload::writeView, AnalyserViewPayload::readView);

    public static final StreamCodec<RegistryFriendlyByteBuf, AnalyserViewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AnalyserViewPayload::containerId,
                    VIEW_CODEC, AnalyserViewPayload::view,
                    AnalyserViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void writeView(final RegistryFriendlyByteBuf buffer, final AnalyserView view) {
        NeoForgeStreamCodecs.enumCodec(AnalyserView.Kind.class).encode(buffer, view.kind());
        ComponentSerialization.STREAM_CODEC.encode(buffer, view.name());
        NeoForgeStreamCodecs.enumCodec(DeviceRole.class).encode(buffer, view.role());
        NetworkBadgeCodec.write(buffer, view.network());
        buffer.writeVarLong(view.use().drawn());
        buffer.writeVarLong(view.use().supplied());
        buffer.writeVarLong(view.use().tolls());
        buffer.writeVarInt(view.energy().devices());
        buffer.writeVarLong(view.energy().energyStored());
        buffer.writeVarLong(view.energy().energyCapacity());
        buffer.writeVarLong(view.energy().energyInput());
        buffer.writeVarLong(view.energy().energyOutput());
        buffer.writeVarInt(view.lines().size());
        for (AnalyserLine line : view.lines()) {
            ComponentSerialization.STREAM_CODEC.encode(buffer, line.label());
            ComponentSerialization.STREAM_CODEC.encode(buffer, line.value());
        }
    }

    private static AnalyserView readView(final RegistryFriendlyByteBuf buffer) {
        final AnalyserView.Kind kind = NeoForgeStreamCodecs.enumCodec(AnalyserView.Kind.class).decode(buffer);
        final var name = ComponentSerialization.STREAM_CODEC.decode(buffer);
        final DeviceRole role = NeoForgeStreamCodecs.enumCodec(DeviceRole.class).decode(buffer);
        final var network = NetworkBadgeCodec.read(buffer);
        final DeviceEnergyUse use = new DeviceEnergyUse(buffer.readVarLong(), buffer.readVarLong(),
                buffer.readVarLong());
        final int devices = buffer.readVarInt();
        final long stored = buffer.readVarLong();
        final long capacity = buffer.readVarLong();
        final long input = buffer.readVarLong();
        final long output = buffer.readVarLong();
        final int lineCount = Math.min(buffer.readVarInt(), AnalyserLines.MAX_LINES);
        final List<AnalyserLine> lines = new ArrayList<>(lineCount);
        for (int index = 0; index < lineCount; index++) {
            lines.add(new AnalyserLine(ComponentSerialization.STREAM_CODEC.decode(buffer),
                    ComponentSerialization.STREAM_CODEC.decode(buffer)));
        }
        return new AnalyserView(kind, name, role, network, use,
                new NetworkStatistics(devices, Map.of(), stored, capacity, input, output), lines);
    }
}
