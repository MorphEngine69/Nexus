package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.FilterMenu;
import com.morphengine.nexus.networking.FilterSlotPayload;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The ghost slots of a filter in a panel, in rows. A slot clicked with an item,
 * with either button, lists what the item stands for in this filter: the fluid
 * in a filled container where fluids are listed, or with Shift the item
 * itself. A click with an empty cursor clears the slot. Nothing is taken or
 * given. A filter that lists nothing is fixed: its empty slots show locked and
 * no click or drag changes it.
 *
 * @param <M> the panel's menu
 */
final class FilterGrid<M extends AbstractContainerMenu & FilterMenu> {

    private static final int HOVER_RGB = 0x80FFFFFF;
    private static final int ICON_SIZE = 16;

    private final M menu;
    private final int left;
    private final int top;
    private final int columns;

    /**
     * @param left screen position of the first slot's left edge
     */
    FilterGrid(final M menu, final int left, final int top, final int columns) {
        this.menu = menu;
        this.left = left;
        this.top = top;
        this.columns = columns;
    }

    PanelBounds slot(final int index) {
        return new PanelBounds(left + index % columns * PanelStyle.SLOT_SIZE,
                top + index / columns * PanelStyle.SLOT_SIZE, PanelStyle.SLOT_SIZE, PanelStyle.SLOT_SIZE);
    }

    void draw(final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final boolean locked = isLocked();
        for (int index = 0; index < menu.filterSlotCount(); index++) {
            final PanelBounds bounds = slot(index);
            final NexusResource resource = menu.filter().resourceAt(index);
            if (resource == null && locked) {
                style.drawLockedSlot(graphics, bounds.left(), bounds.top());
                continue;
            }
            style.drawSlot(graphics, bounds.left(), bounds.top());
            if (resource != null) {
                ResourceRenderers.icon(resource).draw(graphics, bounds.left() + 1, bounds.top() + 1);
            }
            if (!locked && bounds.contains(mouseX, mouseY)) {
                graphics.fill(bounds.left() + 1, bounds.top() + 1, bounds.left() + 1 + ICON_SIZE,
                        bounds.top() + 1 + ICON_SIZE, HOVER_RGB);
            }
        }
    }

    /**
     * @return index of the slot under the cursor; -1 when there is none
     */
    int slotAt(final double x, final double y) {
        for (int index = 0; index < menu.filterSlotCount(); index++) {
            if (slot(index).contains(x, y)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * @return the resource in the slot under the cursor; {@code null} for an empty slot or none
     */
    @Nullable NexusResource resourceAt(final double x, final double y) {
        final int index = slotAt(x, y);
        return index < 0 ? null : menu.filter().resourceAt(index);
    }

    /**
     * @return whether the click hit a slot of the filter
     */
    boolean click(final MouseButtonEvent event) {
        final int index = slotAt(event.x(), event.y());
        if (index < 0) {
            return false;
        }
        if (isLocked()) {
            return true;
        }
        final ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            send(index, null);
            return true;
        }
        final NexusResource resource = event.hasShiftDown()
                ? menu.filterKinds().itemOf(carried) : menu.filterKinds().contentsOf(carried);
        if (resource != null) {
            send(index, resource);
        }
        return true;
    }

    /**
     * @return the slots something may be dragged onto; none while the filter is locked
     */
    List<Rect2i> areas() {
        if (isLocked()) {
            return List.of();
        }
        final List<Rect2i> areas = new ArrayList<>(menu.filterSlotCount());
        for (int index = 0; index < menu.filterSlotCount(); index++) {
            areas.add(slot(index).toRect());
        }
        return areas;
    }

    @Nullable NexusResource entryOf(final ItemStack stack) {
        return menu.filterKinds().itemOf(stack);
    }

    @Nullable NexusResource entryOf(final FluidStack fluid) {
        return menu.filterKinds().fluidOf(fluid);
    }

    private boolean isLocked() {
        return !menu.filterKinds().listsAnything();
    }

    void send(final int index, final @Nullable NexusResource resource) {
        ClientPacketDistributor.sendToServer(new FilterSlotPayload(menu.containerId, index, resource));
    }
}
