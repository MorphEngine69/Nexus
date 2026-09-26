package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkStatistics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.EnumMap;
import java.util.Map;

/**
 * Server to client: new figures for the Nexus menu with the given container id.
 */
public record NexusStatisticsPayload(int containerId, NetworkStatistics statistics) implements CustomPacketPayload {

    public static final Type<NexusStatisticsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "nexus_statistics"));

    private static final StreamCodec<RegistryFriendlyByteBuf, NetworkStatistics> STATISTICS_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NetworkStatistics::devices,
                    ByteBufCodecs.<RegistryFriendlyByteBuf, DeviceRole, Integer, Map<DeviceRole, Integer>>map(
                            size -> new EnumMap<>(DeviceRole.class), NeoForgeStreamCodecs.enumCodec(DeviceRole.class),
                            ByteBufCodecs.VAR_INT, DeviceRole.values().length), NetworkStatistics::devicesByRole,
                    ByteBufCodecs.VAR_LONG, NetworkStatistics::energyStored,
                    ByteBufCodecs.VAR_LONG, NetworkStatistics::energyCapacity,
                    ByteBufCodecs.VAR_LONG, NetworkStatistics::energyInput,
                    ByteBufCodecs.VAR_LONG, NetworkStatistics::energyOutput,
                    NetworkStatistics::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, NexusStatisticsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NexusStatisticsPayload::containerId,
                    STATISTICS_CODEC, NexusStatisticsPayload::statistics,
                    NexusStatisticsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
