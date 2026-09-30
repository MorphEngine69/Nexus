package com.morphengine.nexus.client;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.client.model.CableArmsModel;
import com.morphengine.nexus.client.render.StorageVaultRenderer;
import com.morphengine.nexus.client.screen.AssemblerScreen;
import com.morphengine.nexus.client.screen.CoalGeneratorScreen;
import com.morphengine.nexus.client.screen.CraftingMonitorScreen;
import com.morphengine.nexus.client.screen.EnergyCellScreen;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
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
        event.register(NexusMenuTypes.STORAGE_VAULT.get(), StorageVaultScreen::new);
        event.register(NexusMenuTypes.VAULT_CELL.get(), VaultCellScreen::new);
        event.register(NexusMenuTypes.TERMINAL.get(), TerminalScreen<TerminalMenu>::new);
        event.register(NexusMenuTypes.CRAFTING_TERMINAL.get(), TerminalScreen<CraftingTerminalMenu>::new);
        event.register(NexusMenuTypes.TRANSFER_DEVICE.get(), TransferDeviceScreen::new);
        event.register(NexusMenuTypes.ASSEMBLER.get(), AssemblerScreen::new);
        event.register(NexusMenuTypes.CRAFTING_MONITOR.get(), CraftingMonitorScreen::new);
        event.register(NexusMenuTypes.BLUEPRINT_TERMINAL.get(), TerminalScreen<BlueprintTerminalMenu>::new);
    }

    @SubscribeEvent
    static void registerBlockStateModels(final RegisterBlockStateModels event) {
        event.registerModel(CableArmsModel.ID, CableArmsModel.Unbaked.CODEC);
    }

    @SubscribeEvent
    static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NexusBlockEntityTypes.STORAGE_VAULT.get(),
                context -> new StorageVaultRenderer());
    }
}
