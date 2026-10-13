package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintExecutor;
import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The blueprints of a network as they stood when it was built, looked up by what they give. Nothing in it changes
 * afterwards, so any thread may read it while the registry it came from goes on changing.
 */
final class BlueprintIndex implements BlueprintProvider {

    static final BlueprintIndex EMPTY = new BlueprintIndex(Map.of(), Map.of(), Set.of());

    private final Map<ResourceKey, List<Blueprint>> byOutput;
    private final Map<Blueprint, List<BlueprintExecutor>> executors;
    private final Set<ResourceKey> craftables;

    private BlueprintIndex(
            final Map<ResourceKey, List<Blueprint>> byOutput,
            final Map<Blueprint, List<BlueprintExecutor>> executors,
            final Set<ResourceKey> craftables) {
        this.byOutput = byOutput;
        this.executors = executors;
        this.craftables = craftables;
    }

    /**
     * @param offered each blueprint with the executors that offer it, the preferred executor first
     * @param priorityOf the priority a blueprint is offered at, the best of its executors
     */
    static BlueprintIndex of(
            final Map<Blueprint, List<BlueprintExecutor>> offered, final Map<Blueprint, Integer> priorityOf) {
        final Map<ResourceKey, List<Blueprint>> byOutput = new HashMap<>();
        final Map<Blueprint, List<BlueprintExecutor>> executors = new LinkedHashMap<>();
        final Set<ResourceKey> craftables = new LinkedHashSet<>();
        for (Map.Entry<Blueprint, List<BlueprintExecutor>> entry : offered.entrySet()) {
            final Blueprint blueprint = entry.getKey();
            executors.put(blueprint, List.copyOf(entry.getValue()));
            for (ResourceKey output : outputsOf(blueprint)) {
                craftables.add(output);
                insertByPriority(byOutput.computeIfAbsent(output, key -> new ArrayList<>()), blueprint, priorityOf);
            }
        }
        byOutput.replaceAll((resource, found) -> Collections.unmodifiableList(found));
        return new BlueprintIndex(Collections.unmodifiableMap(byOutput), Collections.unmodifiableMap(executors),
                Collections.unmodifiableSet(craftables));
    }

    @Override
    public List<Blueprint> blueprintsFor(final ResourceKey resource) {
        return byOutput.getOrDefault(resource, List.of());
    }

    @Override
    public List<BlueprintExecutor> executorsFor(final Blueprint blueprint) {
        return executors.getOrDefault(blueprint, List.of());
    }

    @Override
    public Set<ResourceKey> craftables() {
        return craftables;
    }

    private static Set<ResourceKey> outputsOf(final Blueprint blueprint) {
        final Set<ResourceKey> outputs = new LinkedHashSet<>();
        for (ResourceAmount output : blueprint.outputs()) {
            if (blueprint.outputOf(output.resource()) > 0) {
                outputs.add(output.resource());
            }
        }
        return outputs;
    }

    private static void insertByPriority(
            final List<Blueprint> found, final Blueprint blueprint, final Map<Blueprint, Integer> priorityOf) {
        final int priority = priorityOf.get(blueprint);
        int index = 0;
        while (index < found.size() && priorityOf.get(found.get(index)) >= priority) {
            index++;
        }
        found.add(index, blueprint);
    }
}
