package com.morphengine.nexus.terminal;

import com.morphengine.nexus.resource.NexusResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a terminal menu on the client knows of the network's storage, as the
 * server tells it: what is stored, what the network can craft, and the last
 * crafting plan the player asked for. Every change of the listing bumps the
 * revision, so a screen rebuilds its sorted view only when something changed.
 * A resource the network can craft is listed even when none is stored.
 * Client thread only.
 */
public final class TerminalContents {

    private final Map<NexusResource, Long> amounts = new LinkedHashMap<>();
    private Set<NexusResource> craftables = Set.of();
    private TerminalStatus status = TerminalStatus.NO_NETWORK;
    private int revision;
    private @Nullable PlanPreview plan;
    private CraftRequest planOutcome = CraftRequest.PREVIEW;
    private int planRevision;

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

    public void acceptCraftables(final List<NexusResource> received) {
        craftables = Set.copyOf(received);
        revision++;
    }

    public boolean isCraftable(final NexusResource resource) {
        return craftables.contains(resource);
    }

    public void acceptPlan(final PlanPreview received, final CraftRequest outcome) {
        plan = received;
        planOutcome = outcome;
        planRevision++;
    }

    /**
     * @return the plan last received; {@code null} before any
     */
    public @Nullable PlanPreview plan() {
        return plan;
    }

    /**
     * @return whether the plan last received started as a task
     */
    public CraftRequest planOutcome() {
        return planOutcome;
    }

    /**
     * @return a number that changes with every plan received
     */
    public int planRevision() {
        return planRevision;
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
     * @return every known resource with its amount, in the order first heard of,
     *         then what the network can craft and does not store, with none; a snapshot
     */
    public List<TerminalEntry> entries() {
        final List<TerminalEntry> entries = new ArrayList<>(amounts.size() + craftables.size());
        for (Map.Entry<NexusResource, Long> entry : amounts.entrySet()) {
            entries.add(new TerminalEntry(entry.getKey(), entry.getValue()));
        }
        for (NexusResource craftable : craftables) {
            if (!amounts.containsKey(craftable)) {
                entries.add(new TerminalEntry(craftable, 0));
            }
        }
        return entries;
    }
}
