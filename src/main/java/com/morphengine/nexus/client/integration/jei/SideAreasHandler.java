package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.client.screen.SideAreas;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/**
 * Keeps JEI's overlay off the buttons and windows beside the screens of Nexus that have them.
 */
final class SideAreasHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    @Override
    public List<Rect2i> getGuiExtraAreas(final AbstractContainerScreen<?> screen) {
        return screen instanceof SideAreas areas ? areas.extraAreas() : List.of();
    }
}
