package com.morphengine.nexus.menu;

import com.morphengine.nexus.resource.FluidKey;
import org.jspecify.annotations.Nullable;

/**
 * What the panel of a machine shows of its tank, in millibuckets.
 *
 * @param fluid    what is in the tank; {@code null} when it is empty
 * @param capacity what the tank holds
 */
public record TankView(@Nullable FluidKey fluid, long amount, long capacity) {
}
