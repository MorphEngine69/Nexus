package com.morphengine.nexus.client;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.client.screen.CoalGeneratorScreen;
import com.morphengine.nexus.client.screen.EnergyCellScreen;
import com.morphengine.nexus.client.screen.NexusScreen;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Nexus.MOD_ID, value = Dist.CLIENT)
public final class NexusClientSetup {

    private NexusClientSetup() {
    }

    @SubscribeEvent
    static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(NexusMenuTypes.NEXUS.get(), NexusScreen::new);
        event.register(NexusMenuTypes.ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(NexusMenuTypes.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
    }
}
