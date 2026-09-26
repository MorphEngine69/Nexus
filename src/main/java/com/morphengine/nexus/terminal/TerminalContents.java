package com.morphengine.nexus.terminal;

import com.morphengine.nexus.resource.NexusResource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a terminal menu on the client knows of the network's storage, as the
 * server tells it. Every change bumps the revision, so a screen rebuilds its
 * sorted view only when something changed. Client thread only.
 */
public final class TerminalContents {

    private final Map<NexusResource, Long> amounts = new LinkedHashMap<>();
    private TerminalStatus status = TerminalStatus.NO_NETWORK;
    private int revision;

    public void accept(final TerminalStatus newStatus, final boolean reset, final List<TerminalEntry> entries) {
        status = newStatus;
        if (reset) {
            amounts.clear();
        }
        for (TerminalEntry entry : entries) {
            if (entry.amount() == 0) {
                amounts.remove(entry.resource());
            } else {
                amounts.put(entry.resource(), entry.amount());
            }
        }
        revision++;
    }

    public TerminalStatus status() {
        return status;
    }

    public int revision() {
        return revision;
    }

    public long amountOf(final NexusResource resource) {
        return amounts.getOrDefault(resource, 0L);
    }

    /**
     * @return every known resource with its amount, in the order first heard of; a snapshot
     */
    public List<TerminalEntry> entries() {
        final List<TerminalEntry> entries = new ArrayList<>(amounts.size());
        for (Map.Entry<NexusResource, Long> entry : amounts.entrySet()) {
            entries.add(new TerminalEntry(entry.getKey(), entry.getValue()));
        }
        return entries;
    }
}
