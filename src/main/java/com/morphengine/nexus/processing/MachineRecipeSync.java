package com.morphengine.nexus.processing;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.registry.NexusRecipes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Sends the recipes of the machines to the players, which the game does not do for a recipe type of a mod: a client
 * has no other way to know them, and the recipe viewers, JEI and REI, list them from what it knows.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class MachineRecipeSync {

    private MachineRecipeSync() {
    }

    @SubscribeEvent
    static void sendMachineRecipes(final OnDatapackSyncEvent event) {
        event.sendRecipes(NexusRecipes.CRUSHING.get(), NexusRecipes.PULVERIZING.get(),
                NexusRecipes.COMPRESSING.get(), NexusRecipes.ALLOYING.get(), NexusRecipes.EXTRACTING.get());
    }
}
