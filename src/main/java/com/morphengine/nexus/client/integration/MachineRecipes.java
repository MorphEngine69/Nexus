package com.morphengine.nexus.client.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * The recipes of the machines as the client knows them, for the recipe viewers: every recipe the server sent when the
 * player joined or the data packs reloaded.
 */
public final class MachineRecipes {

    private MachineRecipes() {
    }

    /**
     * @return the recipes of {@code type}, every one the client knows
     */
    public static <I extends RecipeInput, R extends Recipe<I>> List<RecipeHolder<R>> of(final RecipeType<R> type) {
        final ClientLevel level = Minecraft.getInstance().level;
        return level == null ? List.of() : List.copyOf(level.getRecipeManager().getAllRecipesFor(type));
    }
}
