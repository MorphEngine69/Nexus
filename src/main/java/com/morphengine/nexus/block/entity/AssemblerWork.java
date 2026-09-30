package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.blueprint.CraftingBlueprint;
import com.morphengine.nexus.blueprint.GridCrafting;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.storage.ResourceCounter;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * What an Assembler does with the runs handed to it and with what they give.
 * A crafting run is crafted on the spot, its outputs held until the network
 * takes them; a processing run goes into the machine the Assembler faces,
 * inputs all at once, and whatever the network's tasks wait for is taken back
 * out of the machine, as far as the side it faces lets it. Server thread only.
 */
final class AssemblerWork {

    /** Crafted units held before crafting stops, until the network takes some; placeholder balance. */
    static final long MAX_HELD_OUTPUTS = 1024;
    /** Steps of a resource taken back from the machine at most per operation; placeholder balance. */
    private static final long STEPS_COLLECTED = 64;

    private final ResourceCounter crafted = new ResourceCounter();

    /**
     * @param inputs the items picked for the run
     */
    DispatchResult craft(final ServerLevel level, final CraftingBlueprint blueprint,
                         final List<ResourceAmount> inputs, final Action action) {
        if (crafted.total() >= MAX_HELD_OUTPUTS) {
            return DispatchResult.TARGET_FULL;
        }
        if (!GridCrafting.crafts(level, blueprint, inputs)) {
            return DispatchResult.NO_TARGET;
        }
        if (action.isExecute()) {
            for (ResourceAmount output : blueprint.blueprint().outputs()) {
                crafted.add(output.resource(), output.amount());
            }
        }
        return DispatchResult.ACCEPTED;
    }

    /**
     * @param inputs the resources picked for the run
     */
    DispatchResult process(final SideStorage machine, final List<ResourceAmount> inputs, final Action action) {
        if (!machine.isPresent()) {
            return DispatchResult.NO_TARGET;
        }
        return machine.insertAll(inputs, action) ? DispatchResult.ACCEPTED : DispatchResult.TARGET_FULL;
    }

    /**
     * @return whether the machine holds anything an input of {@code blueprints} accepts
     */
    boolean isBusy(final SideStorage machine, final List<Blueprint> blueprints) {
        for (ResourceAmount held : machine.contents()) {
            if (anyInputAccepts(blueprints, held.resource())) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyInputAccepts(final List<Blueprint> blueprints, final ResourceKey resource) {
        for (Blueprint blueprint : blueprints) {
            for (BlueprintInput input : blueprint.inputs()) {
                if (input.accepts(resource)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Puts what was crafted into the network, as much as it takes.
     *
     * @return whether anything went in
     */
    boolean deliver(final Storage network) {
        boolean changed = false;
        for (ResourceAmount output : crafted.contents()) {
            final long inserted = network.insert(output.resource(), output.amount(), Action.EXECUTE, Actor.NOBODY);
            if (inserted > 0) {
                crafted.remove(output.resource(), inserted);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Takes the outputs of {@code blueprints} the network's tasks wait for out
     * of the machine and into the network.
     *
     * @return whether anything was taken
     */
    boolean collect(final SideStorage machine, final List<Blueprint> blueprints,
                    final AutocraftingComponent autocrafting, final Storage network) {
        boolean changed = false;
        for (Blueprint blueprint : blueprints) {
            for (ResourceAmount output : blueprint.outputs()) {
                changed |= collect(machine, output, autocrafting, network);
            }
        }
        return changed;
    }

    private boolean collect(final SideStorage machine, final ResourceAmount output,
                            final AutocraftingComponent autocrafting, final Storage network) {
        final long step = NexusResources.of(output.resource()).type().unit().step();
        final long wanted = Math.min(autocrafting.awaited(output.resource()), step * STEPS_COLLECTED);
        final long available = wanted > 0 ? machine.extract(output.resource(), wanted, Action.SIMULATE,
                Actor.NOBODY) : 0;
        final long room = available > 0 ? network.insert(output.resource(), available, Action.SIMULATE,
                Actor.NOBODY) : 0;
        if (room <= 0) {
            return false;
        }
        final long taken = machine.extract(output.resource(), room, Action.EXECUTE, Actor.NOBODY);
        if (taken > 0) {
            network.insert(output.resource(), taken, Action.EXECUTE, Actor.NOBODY);
        }
        return taken > 0;
    }

    /**
     * @return what was crafted and not delivered yet
     */
    List<ResourceAmount> held() {
        return crafted.contents();
    }

    /**
     * Replaces what is held with {@code saved}.
     */
    void restore(final List<ResourceAmount> saved) {
        crafted.clear();
        for (ResourceAmount amount : saved) {
            crafted.add(amount.resource(), amount.amount());
        }
    }

    /**
     * @return what was held; nothing is held afterwards
     */
    List<ResourceAmount> takeAll() {
        final List<ResourceAmount> held = crafted.contents();
        crafted.clear();
        return held;
    }
}
