package com.morphengine.nexus.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.Nexus;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
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

    private static final String DATA_NAME = Nexus.MOD_ID + "_wireless_links";
    private static final String DATA_TAG = "data";
    private static final SavedData.Factory<WirelessLinks> FACTORY = new SavedData.Factory<>(WirelessLinks::new,
            WirelessLinks::load, null);

    private final Map<GlobalPos, GlobalPos> receiverOf = new HashMap<>();

    private WirelessLinks() {
    }

    private WirelessLinks(final List<Link> saved) {
        for (Link link : saved) {
            receiverOf.put(link.transmitter(), link.receiver());
        }
    }

    private static WirelessLinks load(final CompoundTag tag, final HolderLookup.Provider registries) {
        return CODEC.parse(NbtOps.INSTANCE, tag.get(DATA_TAG)).result().orElseGet(WirelessLinks::new);
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(DATA_TAG, CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
        return tag;
    }

    /**
     * @return the links of the world {@code server} runs
     */
    public static WirelessLinks of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
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
