package com.morphengine.nexus.networking;

import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.menu.NetworkBadge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

/**
 * Wire format of the network badge shown in device panels. A device outside any
 * network sends no badge.
 */
final class NetworkBadgeCodec {

    private static final StreamCodec<RegistryFriendlyByteBuf, NetworkBadge> BADGE = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NetworkBadge::name,
            ByteBufCodecs.INT.map(NetworkColor::new, NetworkColor::rgb), NetworkBadge::color,
            NetworkBadge::new);

    private NetworkBadgeCodec() {
    }

    static void write(final RegistryFriendlyByteBuf buffer, final @Nullable NetworkBadge badge) {
        buffer.writeBoolean(badge != null);
        if (badge != null) {
            BADGE.encode(buffer, badge);
        }
    }

    static @Nullable NetworkBadge read(final RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? BADGE.decode(buffer) : null;
    }
}
