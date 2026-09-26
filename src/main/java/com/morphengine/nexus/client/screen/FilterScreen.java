package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A panel with filter slots a recipe viewer can drop items and fluids on. Every
 * panel with a filter implements it, so dragging from a recipe viewer works on
 * each new one without code of its own.
 */
public interface FilterScreen {

    /**
     * @return where the filter slots are on the screen, in slot order
     */
    List<Rect2i> filterSlotAreas();

    /**
     * @return what a dragged {@code stack} lists in the filter; {@code null}
     *         when it lists nothing there
     */
    @Nullable NexusResource filterEntryOf(ItemStack stack);

    /**
     * @return what a dragged {@code fluid} lists in the filter; {@code null}
     *         when the filter lists no fluids
     */
    @Nullable NexusResource filterEntryOf(FluidStack fluid);

    /**
     * Lists {@code resource} in filter slot {@code slot}, as a click with it would.
     */
    void setFilterSlot(int slot, NexusResource resource);
}
