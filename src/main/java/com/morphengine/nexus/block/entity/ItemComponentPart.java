package com.morphengine.nexus.block.entity;

import net.minecraft.core.component.DataComponentMap;

/**
 * A piece of what a device carries on its item when it is taken down, besides its name, and takes back when the item
 * is put up again.
 */
public interface ItemComponentPart {

    /**
     * Puts what the device holds on the item it drops. The loot table of the block says which components the drop
     * copies.
     */
    void collectInto(DataComponentMap.Builder components);

    /**
     * Takes in what the item the device was put up from carries.
     */
    void applyFrom(ComponentSource components);
}
