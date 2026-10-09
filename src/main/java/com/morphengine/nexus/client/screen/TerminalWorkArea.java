package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.client.input.MouseButtonEvent;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What a terminal panel shows between its list and the inventory, beside the
 * slots of its menu: the crafting grid's arrow and clear button, or the
 * Blueprint encoder. Ghost slots it has take what is dragged from a recipe
 * viewer, as a filter does.
 */
interface TerminalWorkArea {

    void draw(GuiGraphics graphics, PanelStyle style, int mouseX, int mouseY);

    /**
     * @return the tooltip of what is under the cursor; empty when there is nothing
     */
    List<Component> tooltip(int mouseX, int mouseY);

    /**
     * @return whether the click hit the area
     */
    boolean click(MouseButtonEvent event);

    /**
     * @return whether the wheel turned over the area
     */
    default boolean scroll(final double x, final double y, final double amount) {
        return false;
    }

    /**
     * @return the ghost slots something may be dragged onto, in slot order
     */
    default List<Rect2i> ghostAreas() {
        return List.of();
    }

    default @Nullable NexusResource ghostEntryOf(final ItemStack stack) {
        return null;
    }

    default @Nullable NexusResource ghostEntryOf(final FluidStack fluid) {
        return null;
    }

    /**
     * Lists {@code resource} in the ghost slot {@code slot}, as a click with it would.
     */
    default void setGhost(final int slot, final NexusResource resource) {
    }
}
