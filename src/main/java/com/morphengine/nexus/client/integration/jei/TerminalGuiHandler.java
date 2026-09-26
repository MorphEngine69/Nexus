package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;
import java.util.Optional;

/**
 * Keeps JEI's overlay off the buttons beside a terminal and shows JEI the
 * resource under the cursor in the terminal's grid.
 */
final class TerminalGuiHandler implements IGuiContainerHandler<TerminalScreen<?>> {

    @Override
    public List<Rect2i> getGuiExtraAreas(final TerminalScreen<?> screen) {
        return List.of(screen.sidebarArea());
    }

    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
            final IClickableIngredientFactory factory, final TerminalScreen<?> screen,
            final double mouseX, final double mouseY) {
        final TerminalScreen.HoveredResource hovered = screen.hoveredResource(mouseX, mouseY);
        if (hovered == null) {
            return Optional.empty();
        }
        return switch (hovered.resource()) {
            case ItemKey item -> factory.createBuilder(item.toStack(1)).buildWithArea(hovered.area());
            case FluidKey fluid -> factory.createBuilder(NeoForgeTypes.FLUID_STACK,
                    fluid.toStack(FluidType.BUCKET_VOLUME)).buildWithArea(hovered.area());
            default -> Optional.empty();
        };
    }
}
