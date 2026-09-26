package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.client.screen.TransferDeviceScreen;
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
 * Crafting Terminal, it keeps its overlay off the side buttons of terminals,
 * Pullers and Pushers, its recipe and usage keys work on the resources a
 * terminal lists, and its items and fluids can be dragged onto the filter of
 * any panel with one.
 */
@REIPluginClient
public final class NexusReiPlugin implements REIClientPlugin {

    @Override
    public void registerTransferHandlers(final TransferHandlerRegistry registry) {
        registry.register(new CraftingTerminalTransferHandler());
    }

    @Override
    public void registerExclusionZones(final ExclusionZones zones) {
        zones.register(TerminalScreen.class, (TerminalScreen<?> screen) -> List.of(rectangle(screen.sidebarArea())));
        zones.register(TransferDeviceScreen.class,
                (TransferDeviceScreen screen) -> List.of(rectangle(screen.sidebarArea())));
    }

    private static Rectangle rectangle(final Rect2i area) {
        return new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }

    @Override
    public void registerScreens(final ScreenRegistry registry) {
        registry.registerFocusedStack(new TerminalFocusedStack());
        registry.registerDraggableStackVisitor(new FilterDragVisitor());
    }
}
