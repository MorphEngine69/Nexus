package com.morphengine.nexus.terminal;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.StorageListener;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.networking.TerminalContentsPayload;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.storage.NetworkStorage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps one player's terminal menu in step with the network's storage. It
 * listens to the storage, gathers changes and sends them once per tick; when
 * the terminal goes offline or the network is replaced, it sends a full
 * listing again. Server thread only.
 */
public final class TerminalSession implements StorageListener {

    /** Entries per packet of a full listing, so a huge network never makes one oversized packet. */
    private static final int LISTING_CHUNK = 512;

    private final ServerPlayer viewer;
    private final int containerId;
    private final TerminalBlockEntity terminal;
    private final Map<NexusResource, Long> pending = new LinkedHashMap<>();
    private @Nullable NetworkStorage watched;
    private @Nullable TerminalStatus sentStatus;

    public TerminalSession(final ServerPlayer viewer, final int containerId, final TerminalBlockEntity terminal) {
        this.viewer = viewer;
        this.containerId = containerId;
        this.terminal = terminal;
    }

    /**
     * Sends what changed since the last tick. Called once per tick.
     */
    public void tick() {
        final TerminalStatus status = terminal.status();
        final NetworkStorage storage = terminal.onlineStorage();
        if (status != sentStatus || storage != watched) {
            watch(storage);
            sentStatus = status;
            sendListing(status, storage);
            return;
        }
        if (!pending.isEmpty()) {
            final List<TerminalEntry> changes = new ArrayList<>(pending.size());
            for (Map.Entry<NexusResource, Long> change : pending.entrySet()) {
                changes.add(new TerminalEntry(change.getKey(), change.getValue()));
            }
            pending.clear();
            send(status, false, changes);
        }
    }

    /**
     * Stops listening, for when the menu closes.
     */
    public void close() {
        watch(null);
    }

    @Override
    public void onAmountChanged(final ResourceKey resource, final long amount) {
        pending.put(NexusResources.of(resource), amount);
    }

    private void watch(final @Nullable NetworkStorage storage) {
        if (watched != null) {
            watched.removeListener(this);
        }
        watched = storage;
        pending.clear();
        if (storage != null) {
            storage.addListener(this);
        }
    }

    private void sendListing(final TerminalStatus status, final @Nullable NetworkStorage storage) {
        final List<ResourceAmount> contents = storage != null ? storage.contents() : List.of();
        if (contents.isEmpty()) {
            send(status, true, List.of());
            return;
        }
        for (int start = 0; start < contents.size(); start += LISTING_CHUNK) {
            final List<ResourceAmount> part = contents.subList(start, Math.min(contents.size(), start + LISTING_CHUNK));
            final List<TerminalEntry> entries = new ArrayList<>(part.size());
            for (ResourceAmount amount : part) {
                entries.add(new TerminalEntry(NexusResources.of(amount.resource()), amount.amount()));
            }
            send(status, start == 0, entries);
        }
    }

    private void send(final TerminalStatus status, final boolean reset, final List<TerminalEntry> entries) {
        PacketDistributor.sendToPlayer(viewer, new TerminalContentsPayload(containerId, status, reset, entries));
    }
}
