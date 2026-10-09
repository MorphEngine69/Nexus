package com.morphengine.nexus.block.entity;

import net.minecraft.core.component.DataComponentType;
import org.jspecify.annotations.Nullable;

/**
 * The components an item hands to the block entity of the block it was placed as.
 */
interface ComponentSource {

    <T> @Nullable T get(DataComponentType<? extends T> type);

    <T> T getOrDefault(DataComponentType<? extends T> type, T fallback);
}
