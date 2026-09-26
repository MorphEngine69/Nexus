package com.morphengine.nexus.terminal;

import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

/**
 * A resource of the network's storage as a terminal is told about it.
 *
 * @param amount units the network holds now; zero means the resource is gone
 */
public record TerminalEntry(NexusResource resource, long amount) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalEntry> STREAM_CODEC = StreamCodec.composite(
            NexusResources.STREAM_CODEC, TerminalEntry::resource,
            ByteBufCodecs.VAR_LONG, TerminalEntry::amount,
            TerminalEntry::new);

    public TerminalEntry {
        Objects.requireNonNull(resource, "resource must not be null");
        if (amount < 0) {
            throw new IllegalArgumentException("amount of " + resource + " must not be negative: " + amount);
        }
    }
}
