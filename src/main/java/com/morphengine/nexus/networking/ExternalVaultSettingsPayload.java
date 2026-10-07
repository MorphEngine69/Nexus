package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.external.ExternalVaultSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: the settings of the External Vault whose panel the player looks at changed.
 */
public record ExternalVaultSettingsPayload(int containerId, ExternalVaultSettings settings)
        implements CustomPacketPayload {

    public static final Type<ExternalVaultSettingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "external_vault_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExternalVaultSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ExternalVaultSettingsPayload::containerId,
                    ExternalVaultSettings.STREAM_CODEC, ExternalVaultSettingsPayload::settings,
                    ExternalVaultSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
