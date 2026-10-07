package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.machine.MachineRecipes;
import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.Optional;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.SAND;
import static com.morphengine.nexus.test.TestResources.STONE;

/**
 * Recipes for tests of the machine core: one stone makes two dirt in ten ticks at five FE a tick, two dirt make one
 * sand in four ticks at three FE a tick.
 */
final class MachineTestRecipes {

    static final int STONE_TICKS = 10;
    static final long STONE_ENERGY = 5;
    static final MachineRecipe STONE_TO_DIRT = MachineRecipe.of(
            new ResourceAmount(STONE, 1), new ResourceAmount(DIRT, 2), STONE_TICKS, STONE_ENERGY);
    static final MachineRecipe DIRT_TO_SAND = MachineRecipe.of(
            new ResourceAmount(DIRT, 2), new ResourceAmount(SAND, 1), 4, 3);

    static final MachineRecipes ALL = available -> {
        final ResourceAmount held = available.getFirst();
        if (held.resource().equals(STONE) && held.amount() >= 1) {
            return Optional.of(STONE_TO_DIRT);
        }
        if (held.resource().equals(DIRT) && held.amount() >= 2) {
            return Optional.of(DIRT_TO_SAND);
        }
        return Optional.empty();
    };

    private MachineTestRecipes() {
    }
}
