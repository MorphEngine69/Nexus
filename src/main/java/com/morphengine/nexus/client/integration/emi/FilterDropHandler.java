package com.morphengine.nexus.client.integration.emi;

import com.morphengine.nexus.client.screen.FilterScreen;
import com.morphengine.nexus.resource.NexusResource;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Lets an item or fluid dragged out of EMI be dropped on a filter slot of any panel with a filter. Only the filter
 * entry is set; the player gets nothing.
 */
final class FilterDropHandler implements EmiDragDropHandler<Screen> {

    private static final int SLOT_OUTLINE = 0x8822EE22;

    @Override
    public boolean dropStack(final Screen screen, final EmiIngredient ingredient, final int x, final int y) {
        if (!(screen instanceof FilterScreen filters) || ingredient.getEmiStacks().isEmpty()) {
            return false;
        }
        final NexusResource resource = entryOf(filters, ingredient.getEmiStacks().getFirst());
        if (resource == null) {
            return false;
        }
        final List<Rect2i> areas = filters.filterSlotAreas();
        for (int slot = 0; slot < areas.size(); slot++) {
            if (areas.get(slot).contains(x, y)) {
                filters.setFilterSlot(slot, resource);
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(
            final Screen screen, final EmiIngredient dragged, final GuiGraphics graphics, final int mouseX,
            final int mouseY, final float delta) {
        if (!(screen instanceof FilterScreen filters)) {
            return;
        }
        for (Rect2i area : filters.filterSlotAreas()) {
            graphics.renderOutline(area.getX(), area.getY(), area.getWidth(), area.getHeight(), SLOT_OUTLINE);
        }
    }

    private static @Nullable NexusResource entryOf(final FilterScreen screen, final EmiStack stack) {
        final ItemStack item = stack.getItemStack();
        if (!item.isEmpty()) {
            return screen.filterEntryOf(item);
        }
        final Fluid fluid = stack.getKeyOfType(Fluid.class);
        return fluid == null ? null : screen.filterEntryOf(new FluidStack(fluid, FluidType.BUCKET_VOLUME));
    }
}
