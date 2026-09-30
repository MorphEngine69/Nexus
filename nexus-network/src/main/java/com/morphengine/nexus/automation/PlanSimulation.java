package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.automation.PlannedRuns;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.StorageView;
import com.morphengine.nexus.math.SaturatedMath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a crafting plan has taken, crafted and missed so far, over the
 * network's storage as it stands. The storage is only read. A copy can be
 * worked on and then either dropped or taken over, so the planner can try one
 * blueprint and fall back to another.
 */
final class PlanSimulation {

    private final StorageView storage;
    private final Map<ResourceKey, Long> fromStorage = new LinkedHashMap<>();
    private final Map<ResourceKey, Long> surplus = new LinkedHashMap<>();
    private final Map<Blueprint, Long> runs = new LinkedHashMap<>();
    private final Map<ResourceKey, Long> crafted = new LinkedHashMap<>();
    private final Map<ResourceKey, Long> missing = new LinkedHashMap<>();

    PlanSimulation(final StorageView storage) {
        this.storage = storage;
    }

    PlanSimulation copy() {
        final PlanSimulation copy = new PlanSimulation(storage);
        copy.takeOver(this);
        return copy;
    }

    /**
     * Makes this simulation the same as {@code other} over the same storage.
     */
    void takeOver(final PlanSimulation other) {
        replace(fromStorage, other.fromStorage);
        replace(surplus, other.surplus);
        replace(runs, other.runs);
        replace(crafted, other.crafted);
        replace(missing, other.missing);
    }

    /**
     * Takes up to {@code amount} of what runs gave beyond what was needed,
     * then of what the storage holds and the plan has not taken yet.
     *
     * @return units taken
     */
    long take(final ResourceKey resource, final long amount) {
        final long fromSurplus = Math.min(amount, surplus.getOrDefault(resource, 0L));
        subtract(surplus, resource, fromSurplus);
        final long left = amount - fromSurplus;
        final long inStorage = storage.amountOf(resource) - fromStorage.getOrDefault(resource, 0L);
        final long fromStore = Math.min(left, Math.max(0, inStorage));
        if (fromStore > 0) {
            fromStorage.merge(resource, fromStore, SaturatedMath::add);
        }
        return fromSurplus + fromStore;
    }

    /**
     * Counts {@code count} runs of {@code blueprint}: all their outputs are
     * crafted and kept aside for whatever needs them later.
     */
    void run(final Blueprint blueprint, final long count) {
        runs.merge(blueprint, count, SaturatedMath::add);
        for (ResourceAmount output : blueprint.outputs()) {
            final long amount = SaturatedMath.multiply(output.amount(), count);
            crafted.merge(output.resource(), amount, SaturatedMath::add);
            surplus.merge(output.resource(), amount, SaturatedMath::add);
        }
    }

    void miss(final ResourceKey resource, final long amount) {
        missing.merge(resource, amount, SaturatedMath::add);
    }

    /**
     * @return units missing in all, for telling whether an attempt missed anything more
     */
    long missingTotal() {
        long total = 0;
        for (long amount : missing.values()) {
            total = SaturatedMath.add(total, amount);
        }
        return total;
    }

    CraftingPlan toPlan(final ResourceAmount target) {
        final List<PlannedRuns> planned = new ArrayList<>(runs.size());
        for (Map.Entry<Blueprint, Long> entry : runs.entrySet()) {
            planned.add(new PlannedRuns(entry.getKey(), entry.getValue()));
        }
        return new CraftingPlan(target, planned, amounts(fromStorage), amounts(crafted), amounts(missing));
    }

    private static <K> void replace(final Map<K, Long> target, final Map<K, Long> source) {
        target.clear();
        target.putAll(source);
    }

    private static void subtract(final Map<ResourceKey, Long> amounts, final ResourceKey resource, final long amount) {
        if (amount <= 0) {
            return;
        }
        final long left = amounts.get(resource) - amount;
        if (left == 0) {
            amounts.remove(resource);
        } else {
            amounts.put(resource, left);
        }
    }

    private static List<ResourceAmount> amounts(final Map<ResourceKey, Long> amounts) {
        final List<ResourceAmount> list = new ArrayList<>(amounts.size());
        for (Map.Entry<ResourceKey, Long> entry : amounts.entrySet()) {
            list.add(new ResourceAmount(entry.getKey(), entry.getValue()));
        }
        return list;
    }
}
