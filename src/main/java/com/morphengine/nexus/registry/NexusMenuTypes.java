package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.CoalGeneratorMenu;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.menu.NexusMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class NexusMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Nexus.MOD_ID);

    public static final Supplier<MenuType<NexusMenu>> NEXUS = MENU_TYPES.register(
            "nexus",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new NexusMenu(containerId, inventory, buffer.readBlockPos())));

    public static final Supplier<MenuType<EnergyCellMenu>> ENERGY_CELL = MENU_TYPES.register(
            "energy_cell",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) ->
                            new EnergyCellMenu(containerId, inventory, buffer.readBlockPos())));

    public static final Supplier<MenuType<CoalGeneratorMenu>> COAL_GENERATOR = MENU_TYPES.register(
            "coal_generator",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) ->
                            new CoalGeneratorMenu(containerId, inventory, buffer.readBlockPos())));

    private NexusMenuTypes() {
    }
}
