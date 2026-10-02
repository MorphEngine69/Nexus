package com.morphengine.nexus.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.Nexus;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Network Transmitters of a world and the Network Receiver each one is
 * linked to, in any dimension. A transmitter keeps its own entry up to date;
 * the network graph reads the links both ways, so a change at either end
 * reaches the Nexus at the other. Saved with the world; server thread only.
 */
public final class WirelessLinks extends SavedData {

    private static final Codec<WirelessLinks> CODEC = Link.CODEC.listOf().xmap(WirelessLinks::new,
            WirelessLinks::links);

    private static final SavedDataType<WirelessLinks> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "wireless_links"), WirelessLinks::new, CODEC);

    private final Map<GlobalPos, GlobalPos> receiverOf = new HashMap<>();

    private WirelessLinks() {
    }

    private WirelessLinks(final List<Link> saved) {
        for (Link link : saved) {
            receiverOf.put(link.transmitter(), link.receiver());
        }
    }

    /**
     * @return the links of the world {@code server} runs
     */
    public static WirelessLinks of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Links {@code transmitter} to {@code receiver}, replacing whatever it was linked to.
     */
    public void link(final GlobalPos transmitter, final GlobalPos receiver) {
        Objects.requireNonNull(receiver, "receiver must not be null");
        if (!receiver.equals(receiverOf.put(transmitter, receiver))) {
            setDirty();
        }
    }

    public void unlink(final GlobalPos transmitter) {
        if (receiverOf.remove(transmitter) != null) {
            setDirty();
        }
    }

    /**
     * @return the receiver {@code transmitter} is linked to; {@code null} when it is linked to none
     */
    public @Nullable GlobalPos receiverOf(final GlobalPos transmitter) {
        return receiverOf.get(transmitter);
    }

    /**
     * @return every position linked with {@code end}: its receiver when it is a
     *         transmitter, the transmitters linked to it when it is a receiver
     */
    public List<GlobalPos> partnersOf(final GlobalPos end) {
        final List<GlobalPos> partners = new ArrayList<>();
        final GlobalPos receiver = receiverOf.get(end);
        if (receiver != null) {
            partners.add(receiver);
        }
        for (Map.Entry<GlobalPos, GlobalPos> link : receiverOf.entrySet()) {
            if (link.getValue().equals(end)) {
                partners.add(link.getKey());
            }
        }
        return partners;
    }

    private List<Link> links() {
        final List<Link> links = new ArrayList<>(receiverOf.size());
        receiverOf.forEach((transmitter, receiver) -> links.add(new Link(transmitter, receiver)));
        return links;
    }

    private record Link(GlobalPos transmitter, GlobalPos receiver) {

        static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        GlobalPos.CODEC.fieldOf("transmitter").forGetter(Link::transmitter),
                        GlobalPos.CODEC.fieldOf("receiver").forGetter(Link::receiver))
                .apply(instance, Link::new));
    }
}
