package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.client.screen.FilterScreen;
import com.morphengine.nexus.resource.NexusResource;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lets an item or fluid dragged out of REI be dropped on a filter slot of any
 * panel with a filter. Only the filter entry is set; the player gets nothing.
 */
final class FilterDragVisitor implements DraggableStackVisitor<Screen> {

    @Override
    public <R extends Screen> boolean isHandingScreen(final R screen) {
        return screen instanceof FilterScreen;
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(final DraggingContext<Screen> context, final DraggableStack stack) {
        if (!(context.getScreen() instanceof FilterScreen screen)) {
            return DraggedAcceptorResult.PASS;
        }
        final NexusResource resource = entryOf(screen, stack.getStack().getValue());
        if (resource == null) {
            return DraggedAcceptorResult.PASS;
        }
        final Point cursor = context.getCurrentPosition();
        final List<Rect2i> areas = screen.filterSlotAreas();
        for (int slot = 0; slot < areas.size(); slot++) {
            if (areas.get(slot).contains(cursor.x, cursor.y)) {
                screen.setFilterSlot(slot, resource);
                return DraggedAcceptorResult.CONSUMED;
            }
        }
        return DraggedAcceptorResult.PASS;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(
            final DraggingContext<Screen> context, final DraggableStack stack) {
        if (!(context.getScreen() instanceof FilterScreen screen)
                || entryOf(screen, stack.getStack().getValue()) == null) {
            return Stream.empty();
        }
        final List<Rectangle> rectangles = new ArrayList<>();
        for (Rect2i area : screen.filterSlotAreas()) {
            rectangles.add(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
        }
        return Stream.of(BoundsProvider.ofRectangles(rectangles));
    }

    /**
     * REI hands fluids over as Architectury stacks; the filter keeps the fluid only.
     */
    private static @Nullable NexusResource entryOf(final FilterScreen screen, final Object value) {
        return switch (value) {
            case ItemStack item -> screen.filterEntryOf(item);
            case dev.architectury.fluid.FluidStack fluid -> screen.filterEntryOf(
                new FluidStack(fluid.getFluid(), FluidType.BUCKET_VOLUME));
            default -> null;
        };
    }
}
