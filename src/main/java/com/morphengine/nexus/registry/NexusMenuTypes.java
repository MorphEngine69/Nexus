package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.AssemblerMenu;
import com.morphengine.nexus.menu.BlockTerminalBinding;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.menu.CoalGeneratorMenu;
import com.morphengine.nexus.menu.CraftingMonitorMenu;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.menu.MachineMenu;
import com.morphengine.nexus.menu.NetworkReceiverMenu;
import com.morphengine.nexus.menu.NetworkTransmitterMenu;
import com.morphengine.nexus.menu.NexusLinkMenu;
import com.morphengine.nexus.menu.NexusMenu;
import com.morphengine.nexus.menu.PortableTerminals;
import com.morphengine.nexus.menu.StorageVaultMenu;
import com.morphengine.nexus.menu.TerminalMenu;
import com.morphengine.nexus.menu.TerminalOpening;
import com.morphengine.nexus.menu.TransferDeviceMenu;
import com.morphengine.nexus.menu.VaultCellMenu;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.transfer.TransferKind;
import com.morphengine.nexus.transfer.TransferSettings;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
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

    public static final Supplier<MenuType<MachineMenu>> MACHINE = MENU_TYPES.register(
            "machine",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) ->
                            new MachineMenu(containerId, inventory, buffer.readBlockPos())));

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
                    (containerId, inventory, buffer) -> new TerminalMenu(NexusMenuTypes.TERMINAL.get(), containerId,
                            inventory, blockOpening(inventory, buffer))));

    public static final Supplier<MenuType<CraftingTerminalMenu>> CRAFTING_TERMINAL = MENU_TYPES.register(
            "crafting_terminal",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new CraftingTerminalMenu(
                            NexusMenuTypes.CRAFTING_TERMINAL.get(), containerId, inventory,
                            blockOpening(inventory, buffer))));

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

    public static final Supplier<MenuType<AssemblerMenu>> ASSEMBLER = MENU_TYPES.register(
            "assembler",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new AssemblerMenu(containerId, inventory,
                            buffer.readBlockPos())));

    public static final Supplier<MenuType<CraftingMonitorMenu>> CRAFTING_MONITOR = MENU_TYPES.register(
            "crafting_monitor",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new CraftingMonitorMenu(containerId, inventory,
                            buffer.readBlockPos())));

    public static final Supplier<MenuType<BlueprintTerminalMenu>> BLUEPRINT_TERMINAL = MENU_TYPES.register(
            "blueprint_terminal",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new BlueprintTerminalMenu(
                            NexusMenuTypes.BLUEPRINT_TERMINAL.get(), containerId, inventory,
                            blockOpening(inventory, buffer))));

    /** A Nexus Terminal working as a terminal. */
    public static final Supplier<MenuType<TerminalMenu>> PORTABLE_TERMINAL = MENU_TYPES.register(
            "portable_terminal",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new TerminalMenu(NexusMenuTypes.PORTABLE_TERMINAL.get(),
                            containerId, inventory, PortableTerminals.readOpening(inventory, buffer))));

    /** A Nexus Terminal working as a crafting terminal. */
    public static final Supplier<MenuType<CraftingTerminalMenu>> PORTABLE_CRAFTING_TERMINAL = MENU_TYPES.register(
            "portable_crafting_terminal",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new CraftingTerminalMenu(
                            NexusMenuTypes.PORTABLE_CRAFTING_TERMINAL.get(), containerId, inventory,
                            PortableTerminals.readOpening(inventory, buffer))));

    /** A Nexus Terminal working as a blueprint terminal. */
    public static final Supplier<MenuType<BlueprintTerminalMenu>> PORTABLE_BLUEPRINT_TERMINAL = MENU_TYPES.register(
            "portable_blueprint_terminal",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new BlueprintTerminalMenu(
                            NexusMenuTypes.PORTABLE_BLUEPRINT_TERMINAL.get(), containerId, inventory,
                            PortableTerminals.readOpening(inventory, buffer))));

    public static final Supplier<MenuType<NetworkTransmitterMenu>> NETWORK_TRANSMITTER = MENU_TYPES.register(
            "network_transmitter",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new NetworkTransmitterMenu(containerId, inventory,
                            buffer.readBlockPos())));

    public static final Supplier<MenuType<NetworkReceiverMenu>> NETWORK_RECEIVER = MENU_TYPES.register(
            "network_receiver",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new NetworkReceiverMenu(containerId, inventory,
                            buffer.readBlockPos())));

    public static final Supplier<MenuType<NexusLinkMenu>> NEXUS_LINK = MENU_TYPES.register(
            "nexus_link",
            () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new NexusLinkMenu(containerId, inventory,
                            buffer.readBlockPos())));

    private NexusMenuTypes() {
    }

    /**
     * @return what the client's menu of a terminal block opens with: the
     *         block's position, then its settings, as the block sent them
     */
    private static TerminalOpening blockOpening(final Inventory inventory, final RegistryFriendlyByteBuf buffer) {
        return new TerminalOpening(new BlockTerminalBinding(inventory, buffer.readBlockPos()),
                TerminalSettings.STREAM_CODEC.decode(buffer));
    }
}
