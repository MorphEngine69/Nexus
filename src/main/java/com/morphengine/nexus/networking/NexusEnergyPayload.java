package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.DeviceEnergyUse;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.level.DeviceEnergyRow;
import com.morphengine.nexus.level.NetworkEnergyReport;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * Server to client: who draws and who supplies energy in the network of the Nexus menu with the given container id,
 * while its energy tab is shown.
 */
public record NexusEnergyPayload(int containerId, NetworkEnergyReport report) implements CustomPacketPayload {

    /** The most devices one report carries. */
    public static final int MAX_DEVICES = 500;

    public static final Type<NexusEnergyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "nexus_energy"));

    private static final StreamCodec<RegistryFriendlyByteBuf, DeviceEnergyUse> USE_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DeviceEnergyUse::drawn,
            ByteBufCodecs.VAR_LONG, DeviceEnergyUse::supplied,
            ByteBufCodecs.VAR_LONG, DeviceEnergyUse::tolls,
            DeviceEnergyUse::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, DeviceEnergyRow> ROW_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, DeviceEnergyRow::position,
            ComponentSerialization.STREAM_CODEC, DeviceEnergyRow::name,
            NeoForgeStreamCodecs.enumCodec(DeviceRole.class), DeviceEnergyRow::role,
            USE_CODEC, DeviceEnergyRow::use,
            DeviceEnergyRow::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, NetworkEnergyReport> REPORT_CODEC =
            StreamCodec.composite(
                    USE_CODEC, NetworkEnergyReport::portableTerminals,
                    ROW_CODEC.apply(ByteBufCodecs.list(MAX_DEVICES)), NetworkEnergyReport::devices,
                    NetworkEnergyReport::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, NexusEnergyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NexusEnergyPayload::containerId,
                    REPORT_CODEC, NexusEnergyPayload::report,
                    NexusEnergyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
