package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.processing.AlloyingRecipe;
import com.morphengine.nexus.processing.CompressingRecipe;
import com.morphengine.nexus.processing.CrushingRecipe;
import com.morphengine.nexus.processing.ExtractingRecipe;
import com.morphengine.nexus.processing.PulverizingRecipe;
import com.morphengine.nexus.registry.NexusRecipes;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The recipe types of the machines as JEI knows them: one for each, made once the registries are filled.
 */
final class MachineRecipeTypes {

    static final RecipeType<RecipeHolder<CrushingRecipe>> CRUSHING =
            RecipeType.createFromVanilla(NexusRecipes.CRUSHING.get());
    static final RecipeType<RecipeHolder<PulverizingRecipe>> PULVERIZING =
            RecipeType.createFromVanilla(NexusRecipes.PULVERIZING.get());
    static final RecipeType<RecipeHolder<CompressingRecipe>> COMPRESSING =
            RecipeType.createFromVanilla(NexusRecipes.COMPRESSING.get());
    static final RecipeType<RecipeHolder<AlloyingRecipe>> ALLOYING =
            RecipeType.createFromVanilla(NexusRecipes.ALLOYING.get());
    static final RecipeType<RecipeHolder<ExtractingRecipe>> EXTRACTING =
            RecipeType.createFromVanilla(NexusRecipes.EXTRACTING.get());

    private MachineRecipeTypes() {
    }
}
