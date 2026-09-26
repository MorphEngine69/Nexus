package com.morphengine.nexus.client.integration.jei;

import com.morphengine.nexus.client.screen.FilterScreen;
import com.morphengine.nexus.resource.NexusResource;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets an item or fluid dragged out of JEI be dropped on a filter slot of any
 * panel with a filter. Only the filter entry is set; the player gets nothing.
 *
 * @param <S> the panel
 */
final class FilterGhostHandler<S extends Screen & FilterScreen> implements IGhostIngredientHandler<S> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(
            final S screen, final ITypedIngredient<I> ingredient, final boolean doStart) {
        final NexusResource resource = entryOf(screen, ingredient.getIngredient());
        if (resource == null) {
            return List.of();
        }
        final List<Rect2i> areas = screen.filterSlotAreas();
        final List<Target<I>> targets = new ArrayList<>(areas.size());
        for (int slot = 0; slot < areas.size(); slot++) {
            targets.add(new FilterTarget<>(screen, slot, areas.get(slot), resource));
        }
        return targets;
    }

    private static @Nullable NexusResource entryOf(final FilterScreen screen, final Object ingredient) {
        return switch (ingredient) {
            case ItemStack stack -> screen.filterEntryOf(stack);
            case FluidStack fluid -> screen.filterEntryOf(fluid);
            default -> null;
        };
    }

    @Override
    public void onComplete() {
    }

    /**
     * One filter slot as a place to drop on.
     */
    private record FilterTarget<I>(FilterScreen screen, int slot, Rect2i area, NexusResource resource)
            implements Target<I> {

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(final I ingredient) {
            screen.setFilterSlot(slot, resource);
        }
    }
}
