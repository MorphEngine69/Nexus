package com.morphengine.nexus.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

/**
 * Wire format of a value that may be absent: a flag, then the value if present.
 */
public final class NullableStreamCodec {

    private NullableStreamCodec() {
    }

    public static <B extends FriendlyByteBuf, T> StreamCodec<B, @Nullable T> of(final StreamCodec<? super B, T> codec) {
        return StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeBoolean(value != null);
                    if (value != null) {
                        codec.encode(buffer, value);
                    }
                },
                buffer -> buffer.readBoolean() ? codec.decode(buffer) : null);
    }
}
