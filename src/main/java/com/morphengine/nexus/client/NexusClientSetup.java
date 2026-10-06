package com.morphengine.nexus.client;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.ExternalVaultBlock;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.client.model.CableArmsModel;
import com.morphengine.nexus.client.render.CraftingMonitorRenderer;
import com.morphengine.nexus.client.render.DeviceGeoModel;
import com.morphengine.nexus.client.render.DeviceRenderer;
import com.morphengine.nexus.client.render.EnergyCellRenderer;
import com.morphengine.nexus.client.render.FacedDeviceRenderer;
import com.morphengine.nexus.client.render.GeneratorRenderer;
import com.morphengine.nexus.client.render.KindDeviceRenderer;
import com.morphengine.nexus.client.render.MachineRenderer;
import com.morphengine.nexus.client.render.NexusRenderer;
import com.morphengine.nexus.client.render.StorageVaultRenderer;
import com.morphengine.nexus.client.screen.AssemblerScreen;
import com.morphengine.nexus.client.screen.CraftingMonitorScreen;
import com.morphengine.nexus.client.screen.EnergyCellScreen;
import com.morphengine.nexus.client.screen.ExternalVaultScreen;
import com.morphengine.nexus.client.screen.GeneratorScreen;
import com.morphengine.nexus.client.screen.MachineScreen;
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
import com.morphengine.nexus.registry.NexusFluids;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.transfer.TransferKind;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;
import java.util.function.Predicate;

@EventBusSubscriber(modid = Nexus.MOD_ID, value = Dist.CLIENT)
public final class NexusClientSetup {

    private static final String EXTERNAL_VAULT_ASSET = "external_vault";
    private static final Predicate<BlockState> WIRELESS_WORKS = state -> state.getValue(WirelessBlock.ACTIVE);

    private NexusClientSetup() {
    }

    @SubscribeEvent
    static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(NexusMenuTypes.NEXUS.get(), NexusScreen::new);
        event.register(NexusMenuTypes.ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(NexusMenuTypes.GENERATOR.get(), GeneratorScreen::new);
        event.register(NexusMenuTypes.MACHINE.get(), MachineScreen::new);
        event.register(NexusMenuTypes.STORAGE_VAULT.get(), StorageVaultScreen::new);
        event.register(NexusMenuTypes.VAULT_CELL.get(), VaultCellScreen::new);
        event.register(NexusMenuTypes.TERMINAL.get(), TerminalScreen<TerminalMenu>::new);
        event.register(NexusMenuTypes.CRAFTING_TERMINAL.get(), TerminalScreen<CraftingTerminalMenu>::new);
        event.register(NexusMenuTypes.TRANSFER_DEVICE.get(), TransferDeviceScreen::new);
        event.register(NexusMenuTypes.EXTERNAL_VAULT.get(), ExternalVaultScreen::new);
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
    static void registerFluidModels(final RegisterFluidModelsEvent event) {
        final Material still = new Material(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "block/biofuel_still"));
        final Material flowing = new Material(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "block/biofuel_flow"));
        event.register(new FluidModel.Unbaked(still, flowing, null, null), NexusFluids.BIOFUEL.get(),
                NexusFluids.BIOFUEL_FLOWING.get());
    }

    @SubscribeEvent
    static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.NEXUS.get(), NexusRenderer::new);
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.MACHINE.get(), MachineRenderer::new);
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.TRANSFER_DEVICE.get(),
                context -> new KindDeviceRenderer<>(context, device -> device.kind().getSerializedName(),
                        TransferKind.PLACER.getSerializedName(), state -> state.getValue(TransferDeviceBlock.POWERED)));
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.EXTERNAL_VAULT.get(),
                context -> new KindDeviceRenderer<>(context, vault -> EXTERNAL_VAULT_ASSET, EXTERNAL_VAULT_ASSET,
                        state -> state.getValue(ExternalVaultBlock.POWERED)));
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
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.GENERATOR.get(), GeneratorRenderer::new);
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.CRAFTING_MONITOR.get(), CraftingMonitorRenderer::new);
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.ENERGY_CELL.get(), EnergyCellRenderer::new);
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.STORAGE_VAULT.get(), StorageVaultRenderer::new);
    }
}
