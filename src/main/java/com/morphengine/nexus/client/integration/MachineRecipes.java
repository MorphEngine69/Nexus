package com.morphengine.nexus.client.integration;

import com.morphengine.nexus.Nexus;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The recipes of the machines as the client knows them, for the recipe viewers: those the server sent when the player
 * joined or the data packs reloaded (see {@code MachineRecipeSync}), or, in a single-player world, those of its own
 * server. Kept from the moment they arrive, before the viewers read them, until the player leaves.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID, value = Dist.CLIENT)
public final class MachineRecipes {

    /** Written by the network thread of the game, read by the viewers when they load; replaced whole. */
    private static volatile RecipeMap received = RecipeMap.EMPTY;

    private MachineRecipes() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onReceived(final RecipesReceivedEvent event) {
        received = event.getRecipeMap();
    }

    @SubscribeEvent
    static void onLeft(final ClientPlayerNetworkEvent.LoggingOut event) {
        received = RecipeMap.EMPTY;
    }

    /**
     * @return the recipes of {@code type}, every one the client knows
     */
    public static <I extends RecipeInput, R extends Recipe<I>> List<RecipeHolder<R>> of(final RecipeType<R> type) {
        final List<RecipeHolder<R>> known = new ArrayList<>(received.byType(type));
        if (known.isEmpty() && Minecraft.getInstance().getSingleplayerServer() != null) {
            known.addAll(Minecraft.getInstance().getSingleplayerServer().getRecipeManager().recipeMap().byType(type));
        }
        return List.copyOf(known);
    }
}
