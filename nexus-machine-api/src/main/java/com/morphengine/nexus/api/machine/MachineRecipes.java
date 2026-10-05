package com.morphengine.nexus.api.machine;

import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.List;
import java.util.Optional;

/**
 * The recipes of one kind of machine, such as smelting or crushing. A machine kind, or an addon, supplies its own and
 * the machine core stays the same. Called from the server thread only.
 */
@FunctionalInterface
public interface MachineRecipes {

    /**
     * @param available what lies in the input slots of a line, each resource once with the amount there; empty
     *                  entries are left out
     * @return the recipe the machine would run on these, or empty when there is none; a recipe is only returned if
     *         {@code available} holds at least all of its inputs
     */
    Optional<MachineRecipe> find(List<ResourceAmount> available);
}
