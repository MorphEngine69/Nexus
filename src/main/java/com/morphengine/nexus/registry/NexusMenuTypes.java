package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.CoalGeneratorMenu;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.menu.NexusMenu;
import com.morphengine.nexus.menu.StorageVaultMenu;
import com.morphengine.nexus.menu.TerminalMenu;
import com.morphengine.nexus.menu.TransferDeviceMenu;
import com.morphengine.nexus.menu.VaultCellMenu;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.transfer.TransferKind;
import com.morphengine.nexus.transfer.TransferSettings;
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

    public static final Supplier<MenuType<StorageVaultMenu>> STORAGE_VAULT = MENU_TYPES.register(
            "storage_vault",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) ->
                            new StorageVaultMenu(containerId, inventory, buffer.readBlockPos())));

    public static final Supplier<MenuType<TerminalMenu>> TERMINAL = MENU_TYPES.register(
            "terminal",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new TerminalMenu(containerId, inventory,
                            buffer.readBlockPos(), TerminalSettings.STREAM_CODEC.decode(buffer))));

    public static final Supplier<MenuType<CraftingTerminalMenu>> CRAFTING_TERMINAL = MENU_TYPES.register(
            "crafting_terminal",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new CraftingTerminalMenu(containerId, inventory,
                            buffer.readBlockPos(), TerminalSettings.STREAM_CODEC.decode(buffer))));

    public static final Supplier<MenuType<VaultCellMenu>> VAULT_CELL = MENU_TYPES.register(
            "vault_cell",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new VaultCellMenu(containerId, inventory)));

    public static final Supplier<MenuType<TransferDeviceMenu>> TRANSFER_DEVICE = MENU_TYPES.register(
            "transfer_device",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new TransferDeviceMenu(containerId, inventory,
                            buffer.readBlockPos(), buffer.readEnum(TransferKind.class),
                            TransferSettings.STREAM_CODEC.decode(buffer))));

    private NexusMenuTypes() {
    }
}
