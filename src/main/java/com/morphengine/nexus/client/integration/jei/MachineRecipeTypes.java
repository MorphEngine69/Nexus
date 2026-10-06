package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.processing.AlloyingRecipe;
import com.morphengine.nexus.processing.CompressingRecipe;
import com.morphengine.nexus.processing.CrushingRecipe;
import com.morphengine.nexus.processing.ExtractingRecipe;
import com.morphengine.nexus.processing.PulverizingRecipe;
import com.morphengine.nexus.registry.NexusRecipes;
import mezz.jei.api.recipe.types.IRecipeHolderType;

/**
 * The recipe types of the machines as JEI knows them: one for each, made once the registries are filled.
 */
final class MachineRecipeTypes {

    static final IRecipeHolderType<CrushingRecipe> CRUSHING = IRecipeHolderType.create(NexusRecipes.CRUSHING.get());
    static final IRecipeHolderType<PulverizingRecipe> PULVERIZING =
            IRecipeHolderType.create(NexusRecipes.PULVERIZING.get());
    static final IRecipeHolderType<CompressingRecipe> COMPRESSING =
            IRecipeHolderType.create(NexusRecipes.COMPRESSING.get());
    static final IRecipeHolderType<AlloyingRecipe> ALLOYING = IRecipeHolderType.create(NexusRecipes.ALLOYING.get());
    static final IRecipeHolderType<ExtractingRecipe> EXTRACTING =
            IRecipeHolderType.create(NexusRecipes.EXTRACTING.get());

    private MachineRecipeTypes() {
    }
}
