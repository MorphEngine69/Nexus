package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.terminal.PlanPreview;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One resource of a crafting plan as the request window lists it: units taken
 * from storage, crafted and missing.
 */
record PlanRow(ResourceKey resource, long fromStorage, long crafted, long missing) {

    private static final int FROM_STORAGE = 0;
    private static final int CRAFTED = 1;
    private static final int MISSING = 2;
    private static final int COLUMNS = 3;

    /**
     * @return a row per resource of {@code plan}, those with anything missing first
     */
    static List<PlanRow> rowsOf(final PlanPreview plan) {
        final Map<ResourceKey, long[]> amounts = new LinkedHashMap<>();
        collect(amounts, plan.missing(), MISSING);
        collect(amounts, plan.crafted(), CRAFTED);
        collect(amounts, plan.fromStorage(), FROM_STORAGE);
        final List<PlanRow> rows = new ArrayList<>(amounts.size());
        for (Map.Entry<ResourceKey, long[]> entry : amounts.entrySet()) {
            final long[] values = entry.getValue();
            rows.add(new PlanRow(entry.getKey(), values[FROM_STORAGE], values[CRAFTED], values[MISSING]));
        }
        return rows;
    }

    private static void collect(final Map<ResourceKey, long[]> amounts, final List<ResourceAmount> list,
                                final int column) {
        for (ResourceAmount amount : list) {
            amounts.computeIfAbsent(amount.resource(), key -> new long[COLUMNS])[column] += amount.amount();
        }
    }
}
