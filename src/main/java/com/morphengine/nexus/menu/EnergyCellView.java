package com.morphengine.nexus.menu;

import org.jspecify.annotations.Nullable;

/**
 * What the Energy Cell panel shows. Energy in RF, rates in RF per tick.
 *
 * @param network the network the cell is in; {@code null} when no Nexus is connected
 */
public record EnergyCellView(long stored, long capacity, long input, long output, @Nullable NetworkBadge network) {

    public static final EnergyCellView EMPTY = new EnergyCellView(0, 0, 0, 0, null);
}
