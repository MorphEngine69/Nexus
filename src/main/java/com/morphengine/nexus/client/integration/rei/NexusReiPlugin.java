package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.client.screen.TerminalScreen;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/**
 * REI support: its "+" button lays out a crafting recipe on the grid of a
 * Crafting Terminal, it keeps its overlay off the terminal's side buttons, its
 * recipe and usage keys work on the resources a terminal lists, and its items
 * and fluids can be dragged onto the filter of a Vault Cell.
 */
@REIPluginClient
public final class NexusReiPlugin implements REIClientPlugin {

    @Override
    public void registerTransferHandlers(final TransferHandlerRegistry registry) {
        registry.register(new CraftingTerminalTransferHandler());
    }

    @Override
    public void registerExclusionZones(final ExclusionZones zones) {
        zones.register(TerminalScreen.class, (TerminalScreen<?> screen) -> {
            final Rect2i area = screen.sidebarArea();
            return List.of(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
        });
    }

    @Override
    public void registerScreens(final ScreenRegistry registry) {
        registry.registerFocusedStack(new TerminalFocusedStack());
        registry.registerDraggableStackVisitor(new VaultCellDragVisitor());
    }
}
