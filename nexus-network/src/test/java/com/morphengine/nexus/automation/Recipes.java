package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.storage.CellStorage;
import com.morphengine.nexus.storage.NetworkStorage;

import java.util.List;

import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.ROOMY;
import static com.morphengine.nexus.test.TestResources.item;

/**
 * A small tree of recipes for the tests of autocrafting: a log gives four
 * planks, two planks give four sticks, a stick and coal give four torches.
 * Sticks also come from two planks of any wood, oak preferred, and a spruce
 * log gives four spruce planks.
 */
final class Recipes {

    static final ResourceKey LOG = item("log");
    static final ResourceKey PLANKS = item("planks");
    static final ResourceKey STICK = item("stick");
    static final ResourceKey COAL = item("coal");
    static final ResourceKey TORCH = item("torch");
    static final ResourceKey ORE = item("ore");
    static final ResourceKey INGOT = item("ingot");
    static final ResourceKey SPRUCE_LOG = item("spruce_log");
    static final ResourceKey SPRUCE_PLANKS = item("spruce_planks");
    static final BlueprintInput ANY_PLANKS = new BlueprintInput(List.of(PLANKS, SPRUCE_PLANKS), 2);

    static final Blueprint PLANKS_FROM_LOG = crafting(List.of(amount(LOG, 1)), amount(PLANKS, 4));
    static final Blueprint STICKS_FROM_PLANKS = crafting(List.of(amount(PLANKS, 2)), amount(STICK, 4));
    static final Blueprint TORCHES = crafting(List.of(amount(STICK, 1), amount(COAL, 1)), amount(TORCH, 4));
    static final Blueprint STICKS_FROM_ANY_PLANKS = new Blueprint(BlueprintKind.CRAFTING, List.of(ANY_PLANKS),
            List.of(amount(STICK, 4)));
    static final Blueprint SPRUCE_PLANKS_FROM_LOG = crafting(List.of(amount(SPRUCE_LOG, 1)),
            amount(SPRUCE_PLANKS, 4));
    static final Blueprint SMELTING = Blueprint.exact(BlueprintKind.PROCESSING, List.of(amount(ORE, 1)),
            List.of(amount(INGOT, 1)));

    private Recipes() {
    }

    static ResourceAmount amount(final ResourceKey resource, final long amount) {
        return new ResourceAmount(resource, amount);
    }

    static Blueprint crafting(final List<ResourceAmount> inputs, final ResourceAmount output) {
        return Blueprint.exact(BlueprintKind.CRAFTING, inputs, List.of(output));
    }

    /**
     * @return a network storage of one roomy cell holding {@code contents}
     */
    static NetworkStorage network(final ResourceAmount... contents) {
        final NetworkStorage network = new NetworkStorage();
        network.addSource(new CellStorage(ITEMS, ROOMY, List.of(contents)), 0);
        return network;
    }

    /**
     * Inserts into the network the way a Puller does, so interceptors see it.
     */
    static long deliver(final NetworkStorage network, final ResourceKey resource, final long amount) {
        return network.insert(resource, amount, Action.EXECUTE, Actor.NOBODY);
    }
}
