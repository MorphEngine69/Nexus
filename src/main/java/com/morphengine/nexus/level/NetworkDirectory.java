package com.morphengine.nexus.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.Nexus;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Where the Nexus of every network of a world stands, by the network's id, in
 * any dimension. A Nexus records itself when it starts running and is
 * forgotten when it is broken, not when its chunk unloads, so a network can be
 * found while its Nexus is unloaded, and again after the Nexus has been moved.
 * Saved with the world; server thread only.
 */
public final class NetworkDirectory extends SavedData {

    private static final Codec<NetworkDirectory> CODEC = Entry.CODEC.listOf().xmap(NetworkDirectory::new,
            NetworkDirectory::entries);

    private static final SavedDataType<NetworkDirectory> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "network_directory"), NetworkDirectory::new, CODEC);

    private final Map<UUID, GlobalPos> nexusOf = new HashMap<>();

    private NetworkDirectory() {
    }

    private NetworkDirectory(final List<Entry> saved) {
        for (Entry entry : saved) {
            nexusOf.put(entry.network(), entry.nexus());
        }
    }

    /**
     * @return the directory of the world {@code server} runs
     */
    public static NetworkDirectory of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * @return where the Nexus of {@code network} stands; {@code null} when no
     *         Nexus of that network stands anywhere
     */
    public @Nullable GlobalPos nexusOf(final UUID network) {
        return nexusOf.get(network);
    }

    /**
     * Records that the Nexus of {@code network} stands at {@code nexus},
     * wherever it stood before.
     */
    public void record(final UUID network, final GlobalPos nexus) {
        Objects.requireNonNull(nexus, "nexus must not be null");
        if (!nexus.equals(nexusOf.put(network, nexus))) {
            setDirty();
        }
    }

    /**
     * Forgets the Nexus broken at {@code nexus}, if one is recorded there.
     */
    public void forgetAt(final GlobalPos nexus) {
        if (nexusOf.values().removeIf(nexus::equals)) {
            setDirty();
        }
    }

    /**
     * Records that a Nexus of {@code network} stands at {@code here}, as it does
     * when it starts running. A Nexus whose network another one already leads,
     * such as a copy picked in creative mode, is recorded under a new id
     * instead, so it starts a network of its own rather than taking the other
     * one over.
     *
     * @return the id recorded: {@code network}, or the new one
     */
    public UUID claim(final UUID network, final GlobalPos here, final MinecraftServer server) {
        final UUID claimed = isLeadElsewhere(network, here, server) ? UUID.randomUUID() : network;
        record(claimed, here);
        return claimed;
    }

    /**
     * @return whether a Nexus other than the one at {@code here} may lead
     *         {@code network}: one is recorded elsewhere, and it still stands
     *         there or its chunk is not loaded to tell
     */
    private boolean isLeadElsewhere(final UUID network, final GlobalPos here, final MinecraftServer server) {
        final GlobalPos known = nexusOf.get(network);
        if (known == null || known.equals(here)) {
            return false;
        }
        final ServerLevel level = server.getLevel(known.dimension());
        if (level == null) {
            return false;
        }
        return !level.isLoaded(known.pos())
                || level.getBlockEntity(known.pos()) instanceof NetworkController other
                && other.network().id().equals(network);
    }

    private List<Entry> entries() {
        final List<Entry> entries = new ArrayList<>(nexusOf.size());
        nexusOf.forEach((network, nexus) -> entries.add(new Entry(network, nexus)));
        return entries;
    }

    private record Entry(UUID network, GlobalPos nexus) {

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        UUIDUtil.CODEC.fieldOf("network").forGetter(Entry::network),
                        GlobalPos.CODEC.fieldOf("nexus").forGetter(Entry::nexus))
                .apply(instance, Entry::new));
    }
}
