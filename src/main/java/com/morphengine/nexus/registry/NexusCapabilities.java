package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class NexusCapabilities {

    private NexusCapabilities() {
    }

    @SubscribeEvent
    static void register(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                NexusBlockEntityTypes.ENERGY_CELL.get(),
                (cell, side) -> cell.energyHandler());
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                NexusBlockEntityTypes.COAL_GENERATOR.get(),
                (generator, side) -> generator.energyHandler());
    }
}
