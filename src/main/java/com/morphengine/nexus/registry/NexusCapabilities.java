package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.level.NetworkNeighbours;
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
                Capabilities.EnergyStorage.BLOCK,
                NexusBlockEntityTypes.ENERGY_CELL.get(),
                (cell, side) -> cell.energyHandlerBeyond(side));
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                NexusBlockEntityTypes.GENERATOR.get(),
                (generator, side) -> generator.energyHandler(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                NexusBlockEntityTypes.GENERATOR.get(),
                (generator, side) -> generator.fluidHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NexusBlockEntityTypes.GENERATOR.get(),
                (generator, side) -> generator.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                NexusBlockEntityTypes.MACHINE.get(),
                (machine, side) -> machine.energyHandler());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NexusBlockEntityTypes.MACHINE.get(),
                (machine, side) -> machine.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                NexusBlockEntityTypes.MACHINE.get(),
                (machine, side) -> machine.fluidHandler(side));
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                NexusBlockEntityTypes.NEXUS.get(),
                (nexus, side) -> NetworkNeighbours.offeredBeyond(nexus, side, nexus.energyHandler()));
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, context) -> NexusTerminalItem.chargeStorage(stack),
                NexusItems.NEXUS_TERMINAL.get());
    }
}
