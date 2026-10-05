package com.morphengine.nexus.processing;

import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Optional;

/**
 * The recipes of a kind of machine, which a level supplies: the recipe manager, tags and the like are not there until
 * the machine stands in a world.
 */
@FunctionalInterface
public interface LevelRecipes {

    /**
     * @see com.morphengine.nexus.api.machine.MachineRecipes#find
     */
    Optional<MachineRecipe> find(ServerLevel level, List<ResourceAmount> available);

    /**
     * @return whether the item is used by some recipe, so that an input slot takes it; by default whether it is a
     *         recipe of its own, which a recipe with several inputs overrides
     */
    default boolean usesItem(final ServerLevel level, final ItemKey key) {
        return find(level, List.of(new ResourceAmount(key, 1))).isPresent();
    }
}
