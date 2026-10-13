package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintExecutor;
import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The blueprints of one network, each with the executors that offer it. An
 * executor offers its blueprints at one priority: a blueprint several
 * executors offer is run by the one of the highest priority first, and of
 * several blueprints giving the same resource, the one offered at the highest
 * priority is preferred. On a tie, whoever came first wins. Every change
 * bumps the {@linkplain #revision() revision}. Server thread only.
 */
public final class BlueprintRegistry implements BlueprintProvider {

    private final Map<Blueprint, List<Offer>> offers = new LinkedHashMap<>();
    private int revision;
    private BlueprintIndex lookup = BlueprintIndex.EMPTY;
    private boolean lookupOutdated;

    /**
     * Replaces whatever {@code executor} offered before with {@code blueprints}.
     *
     * @param blueprints the blueprints it offers now; the same blueprint twice counts once
     */
    public void offer(final BlueprintExecutor executor, final List<Blueprint> blueprints, final int priority) {
        Objects.requireNonNull(executor, "executor must not be null");
        withdrawSilently(executor);
        for (Blueprint blueprint : new LinkedHashSet<>(blueprints)) {
            final List<Offer> executors = offers.computeIfAbsent(blueprint, key -> new ArrayList<>());
            int index = 0;
            while (index < executors.size() && executors.get(index).priority() >= priority) {
                index++;
            }
            executors.add(index, new Offer(executor, priority));
        }
        changed();
    }

    /**
     * Takes back everything {@code executor} offered.
     */
    public void withdraw(final BlueprintExecutor executor) {
        if (withdrawSilently(executor)) {
            changed();
        }
    }

    /**
     * @return a number that changes whenever the offered blueprints change
     */
    public int revision() {
        return revision;
    }

    /**
     * @return the blueprints as they stand now, which later changes to this registry do not touch; safe to read on
     *         any thread. Built when first asked for after a change and shared until the next one.
     */
    public BlueprintProvider snapshot() {
        if (lookupOutdated) {
            lookup = buildIndex();
            lookupOutdated = false;
        }
        return lookup;
    }

    @Override
    public List<Blueprint> blueprintsFor(final ResourceKey resource) {
        return snapshot().blueprintsFor(resource);
    }

    @Override
    public List<BlueprintExecutor> executorsFor(final Blueprint blueprint) {
        return snapshot().executorsFor(blueprint);
    }

    @Override
    public Set<ResourceKey> craftables() {
        return snapshot().craftables();
    }

    private void changed() {
        revision++;
        lookupOutdated = true;
    }

    private BlueprintIndex buildIndex() {
        final Map<Blueprint, List<BlueprintExecutor>> executors = new LinkedHashMap<>();
        final Map<Blueprint, Integer> priorities = new HashMap<>();
        for (Map.Entry<Blueprint, List<Offer>> entry : offers.entrySet()) {
            final List<BlueprintExecutor> offering = new ArrayList<>(entry.getValue().size());
            for (Offer offer : entry.getValue()) {
                offering.add(offer.executor());
            }
            executors.put(entry.getKey(), offering);
            priorities.put(entry.getKey(), entry.getValue().getFirst().priority());
        }
        return BlueprintIndex.of(executors, priorities);
    }

    private boolean withdrawSilently(final BlueprintExecutor executor) {
        boolean changed = false;
        final var entries = offers.values().iterator();
        while (entries.hasNext()) {
            final List<Offer> executors = entries.next();
            changed |= executors.removeIf(offer -> offer.executor() == executor);
            if (executors.isEmpty()) {
                entries.remove();
            }
        }
        return changed;
    }

    private record Offer(BlueprintExecutor executor, int priority) {
    }
}
