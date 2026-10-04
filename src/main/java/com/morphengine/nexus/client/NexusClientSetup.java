package com.morphengine.nexus.client;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.CoalGeneratorBlock;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.client.model.CableArmsModel;
import com.morphengine.nexus.client.render.CraftingMonitorRenderer;
import com.morphengine.nexus.client.render.DeviceGeoModel;
import com.morphengine.nexus.client.render.DeviceRenderer;
import com.morphengine.nexus.client.render.EnergyCellRenderer;
import com.morphengine.nexus.client.render.FacedDeviceRenderer;
import com.morphengine.nexus.client.render.KindDeviceRenderer;
import com.morphengine.nexus.client.render.NexusRenderer;
import com.morphengine.nexus.client.render.StorageVaultRenderer;
import com.morphengine.nexus.client.screen.AssemblerScreen;
import com.morphengine.nexus.client.screen.CoalGeneratorScreen;
import com.morphengine.nexus.client.screen.CraftingMonitorScreen;
import com.morphengine.nexus.client.screen.EnergyCellScreen;
import com.morphengine.nexus.client.screen.NetworkReceiverScreen;
import com.morphengine.nexus.client.screen.NetworkTransmitterScreen;
import com.morphengine.nexus.client.screen.NexusLinkScreen;
import com.morphengine.nexus.client.screen.NexusScreen;
import com.morphengine.nexus.client.screen.StorageVaultScreen;
import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.client.screen.TransferDeviceScreen;
import com.morphengine.nexus.client.screen.VaultCellScreen;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.menu.TerminalMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.transfer.TransferKind;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;
import java.util.function.Predicate;

@EventBusSubscriber(modid = Nexus.MOD_ID, value = Dist.CLIENT)
public final class NexusClientSetup {

    private static final Predicate<BlockState> WIRELESS_WORKS = state -> state.getValue(WirelessBlock.ACTIVE);

    private NexusClientSetup() {
    }

    @SubscribeEvent
    static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(NexusMenuTypes.NEXUS.get(), NexusScreen::new);
        event.register(NexusMenuTypes.ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(NexusMenuTypes.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
        event.register(NexusMenuTypes.STORAGE_VAULT.get(), StorageVaultScreen::new);
        event.register(NexusMenuTypes.VAULT_CELL.get(), VaultCellScreen::new);
        event.register(NexusMenuTypes.TERMINAL.get(), TerminalScreen<TerminalMenu>::new);
        event.register(NexusMenuTypes.CRAFTING_TERMINAL.get(), TerminalScreen<CraftingTerminalMenu>::new);
        event.register(NexusMenuTypes.TRANSFER_DEVICE.get(), TransferDeviceScreen::new);
        event.register(NexusMenuTypes.ASSEMBLER.get(), AssemblerScreen::new);
        event.register(NexusMenuTypes.CRAFTING_MONITOR.get(), CraftingMonitorScreen::new);
        event.register(NexusMenuTypes.BLUEPRINT_TERMINAL.get(), TerminalScreen<BlueprintTerminalMenu>::new);
        event.register(NexusMenuTypes.NETWORK_TRANSMITTER.get(), NetworkTransmitterScreen::new);
        event.register(NexusMenuTypes.NETWORK_RECEIVER.get(), NetworkReceiverScreen::new);
        event.register(NexusMenuTypes.NEXUS_LINK.get(), NexusLinkScreen::new);
        event.register(NexusMenuTypes.PORTABLE_TERMINAL.get(), TerminalScreen<TerminalMenu>::new);
        event.register(NexusMenuTypes.PORTABLE_CRAFTING_TERMINAL.get(), TerminalScreen<CraftingTerminalMenu>::new);
        event.register(NexusMenuTypes.PORTABLE_BLUEPRINT_TERMINAL.get(), TerminalScreen<BlueprintTerminalMenu>::new);
    }

    @SubscribeEvent
    static void registerBlockStateModels(final RegisterBlockStateModels event) {
        event.registerModel(CableArmsModel.ID, CableArmsModel.Unbaked.CODEC);
    }

    @SubscribeEvent
    static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.NEXUS.get(), NexusRenderer::new);
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.TRANSFER_DEVICE.get(),
                context -> new KindDeviceRenderer<>(context, device -> device.kind().getSerializedName(),
                        TransferKind.PLACER.getSerializedName(), state -> state.getValue(TransferDeviceBlock.POWERED)));
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.TERMINAL.get(),
                context -> new KindDeviceRenderer<>(context, terminal -> terminal.kind().getSerializedName(),
                        TerminalKind.TERMINAL.getSerializedName(), state -> state.getValue(TerminalBlock.POWERED)));
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.NETWORK_TRANSMITTER.get(),
                context -> new DeviceRenderer<>(context, new DeviceGeoModel<>("network_transmitter"), WIRELESS_WORKS));
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.NETWORK_RECEIVER.get(),
                context -> new DeviceRenderer<>(context, new DeviceGeoModel<>("network_receiver"), WIRELESS_WORKS));
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.NEXUS_LINK.get(),
                context -> new DeviceRenderer<>(context, new DeviceGeoModel<>("nexus_link"), WIRELESS_WORKS));
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.ASSEMBLER.get(),
                context -> new FacedDeviceRenderer<>(context, new DeviceGeoModel<>("assembler"),
                        state -> state.getValue(AssemblerBlock.POWERED), AssemblerBlock.FACING, List.of(),
                        List.of()));
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.COAL_GENERATOR.get(),
                context -> new FacedDeviceRenderer<>(context, new DeviceGeoModel<>("coal_generator"),
                        state -> state.getValue(CoalGeneratorBlock.LIT), CoalGeneratorBlock.FACING,
                        List.of("fins", "flue"), List.of("fire")));
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.CRAFTING_MONITOR.get(), CraftingMonitorRenderer::new);
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.ENERGY_CELL.get(), EnergyCellRenderer::new);
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.STORAGE_VAULT.get(), StorageVaultRenderer::new);
    }
}
