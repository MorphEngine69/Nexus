package com.morphengine.nexus.menu;

import com.morphengine.nexus.filter.FilterKinds;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A menu whose panel edits a filter of ghost slots: a slot only records a
 * resource, nothing is taken from the player or given. The client reads the
 * filter to draw it; the player's changes reach the server as payloads and are
 * applied there.
 */
public interface FilterMenu {

    /**
     * @return the filter as the panel shows it; on the client, as last synchronized
     */
    FilterSlots filter();

    /**
     * @return how many slots of the filter are in use, at most {@value FilterSlots#MAX_SLOTS}
     */
    int filterSlotCount();

    FilterKinds filterKinds();

    /**
     * Stores the filter the player changed. Server side only.
     */
    void changeFilter(FilterSlots changed);

    /**
     * Lists {@code resource} in filter slot {@code slot}, or empties the slot for
     * {@code null}. A locked filter, a slot out of range or a resource of a kind
     * the filter does not list changes nothing. Server side only.
     */
    default void setFilterSlot(final int slot, final @Nullable NexusResource resource) {
        if (!filterKinds().listsAnything() || slot < 0 || slot >= filterSlotCount()) {
            return;
        }
        if (resource != null && !filterKinds().accepts(resource)) {
            return;
        }
        changeFilter(filter().with(slot, resource));
    }

    /**
     * @return whether a slot may list a tag of its resource, standing for every
     *         resource in that tag, instead of the resource itself; known on
     *         both sides
     */
    default boolean listsTags() {
        return false;
    }

    /**
     * Moves filter slot {@code slot} on to the next tag of its resource, or the
     * previous one for a negative {@code step}. A filter without tags, a slot
     * out of range or an empty slot changes nothing. Server side only.
     */
    default void stepFilterTag(final int slot, final int step) {
        if (!listsTags() || slot < 0 || slot >= filterSlotCount() || step == 0) {
            return;
        }
        changeFilter(filter().withNextTag(slot, Integer.signum(step)));
    }

    /**
     * Switches the filter between whitelist and blacklist; a locked filter stays
     * as it is. Server side only.
     */
    default void toggleFilterMode() {
        if (filterKinds().listsAnything()) {
            changeFilter(filter().withMode(filter().mode().toggled()));
        }
    }

    /**
     * Lists what {@code stack} stands for in the first free slot, as a Shift
     * click on it in the inventory does; the stack stays where it is. Nothing
     * changes when it is listed already, lists nothing here, or no slot is
     * free. Server side only.
     */
    default void addToFilter(final ItemStack stack) {
        final NexusResource resource = filterKinds().contentsOf(stack);
        if (resource == null || filter().lists(resource)) {
            return;
        }
        for (int slot = 0; slot < filterSlotCount(); slot++) {
            if (filter().resourceAt(slot) == null) {
                setFilterSlot(slot, resource);
                return;
            }
        }
    }
}
