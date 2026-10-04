package com.morphengine.nexus.access;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.security.Member;
import com.morphengine.nexus.security.NetworkSecurity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The access of every network of a world, by the network's id, wherever its
 * Nexus stands and whether it is loaded or carried around as an item: who may
 * do what with a network does not depend on its Nexus being there. Saved with
 * the world; server thread only.
 */
public final class NetworkSecurityData extends SavedData {

    private static final Codec<NetworkSecurityData> CODEC = Entry.CODEC.listOf().xmap(NetworkSecurityData::new,
            NetworkSecurityData::entries);

    private static final SavedDataType<NetworkSecurityData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "network_security"), NetworkSecurityData::new, CODEC);

    private final Map<UUID, NetworkSecurity> byNetwork = new HashMap<>();

    private NetworkSecurityData() {
    }

    private NetworkSecurityData(final List<Entry> saved) {
        for (Entry entry : saved) {
            byNetwork.put(entry.network(), NetworkSecurity.restore(entry.defaultRole(), entry.members(),
                    this::setDirty));
        }
    }

    /**
     * @return the access data of the world {@code server} runs
     */
    public static NetworkSecurityData of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * @return the access of {@code network}; {@code null} when it has none yet,
     *         as for a network whose Nexus has never run since access rules existed
     */
    public @Nullable NetworkSecurity find(final UUID network) {
        return byNetwork.get(network);
    }

    /**
     * @param founder the player who set the network up; {@code null} when
     *                nobody knows, as for a network from before access rules
     * @return the access of {@code network}, set up now when it has none: owned
     *         by {@code founder}, or without one unclaimed
     */
    public NetworkSecurity establish(final UUID network, final @Nullable NameAndId founder) {
        final NetworkSecurity known = byNetwork.get(network);
        if (known != null) {
            return known;
        }
        final NetworkSecurity created = founder != null
                ? NetworkSecurity.ownedBy(founder.id(), Member.fitName(founder.name(), founder.id()), this::setDirty)
                : NetworkSecurity.unclaimed(this::setDirty);
        byNetwork.put(network, created);
        setDirty();
        return created;
    }

    /**
     * Takes the name {@code player} goes by now into every network they are a member of.
     */
    public void refreshNames(final NameAndId player) {
        for (NetworkSecurity security : byNetwork.values()) {
            security.refreshName(player.id(), player.name());
        }
    }

    private List<Entry> entries() {
        final List<Entry> entries = new ArrayList<>(byNetwork.size());
        byNetwork.forEach((network, security) -> entries.add(
                new Entry(network, security.defaultRole(), security.members())));
        return entries;
    }

    private record Entry(UUID network, Role defaultRole, List<Member> members) {

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        UUIDUtil.CODEC.fieldOf("network").forGetter(Entry::network),
                        SecurityCodecs.ROLE.fieldOf("default_role").forGetter(Entry::defaultRole),
                        SecurityCodecs.MEMBER.listOf().optionalFieldOf("members", List.of())
                                .forGetter(Entry::members))
                .apply(instance, Entry::new));
    }
}
