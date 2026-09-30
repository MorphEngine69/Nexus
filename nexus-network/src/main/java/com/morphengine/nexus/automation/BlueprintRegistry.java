package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintExecutor;
import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.ArrayList;
import java.util.Collections;
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
        revision++;
    }

    /**
     * Takes back everything {@code executor} offered.
     */
    public void withdraw(final BlueprintExecutor executor) {
        if (withdrawSilently(executor)) {
            revision++;
        }
    }

    /**
     * @return a number that changes whenever the offered blueprints change
     */
    public int revision() {
        return revision;
    }

    @Override
    public List<Blueprint> blueprintsFor(final ResourceKey resource) {
        final List<Blueprint> found = new ArrayList<>();
        for (Map.Entry<Blueprint, List<Offer>> entry : offers.entrySet()) {
            if (entry.getKey().outputOf(resource) > 0) {
                insertByPriority(found, entry.getKey());
            }
        }
        return Collections.unmodifiableList(found);
    }

    @Override
    public List<BlueprintExecutor> executorsFor(final Blueprint blueprint) {
        final List<Offer> executors = offers.getOrDefault(blueprint, List.of());
        final List<BlueprintExecutor> found = new ArrayList<>(executors.size());
        for (Offer offer : executors) {
            found.add(offer.executor());
        }
        return Collections.unmodifiableList(found);
    }

    @Override
    public Set<ResourceKey> craftables() {
        final Set<ResourceKey> craftables = new LinkedHashSet<>();
        for (Blueprint blueprint : offers.keySet()) {
            for (ResourceAmount output : blueprint.outputs()) {
                craftables.add(output.resource());
            }
        }
        return Collections.unmodifiableSet(craftables);
    }

    private void insertByPriority(final List<Blueprint> found, final Blueprint blueprint) {
        final int priority = bestPriority(blueprint);
        int index = 0;
        while (index < found.size() && bestPriority(found.get(index)) >= priority) {
            index++;
        }
        found.add(index, blueprint);
    }

    private int bestPriority(final Blueprint blueprint) {
        return offers.get(blueprint).getFirst().priority();
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
