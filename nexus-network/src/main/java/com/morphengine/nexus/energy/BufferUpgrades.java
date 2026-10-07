package com.morphengine.nexus.energy;

import com.morphengine.nexus.math.SaturatedMath;

/**
 * What a Buffer Upgrade does: it adds the base size to what a buffer or a tank holds, so that it holds twice as
 * much. Placeholder balance until the numbers are settled.
 */
public final class BufferUpgrades {

    public static final int MAX_UPGRADES = 1;

    private BufferUpgrades() {
    }

    /**
     * @param base     what the buffer holds without upgrades, not negative
     * @param upgrades Buffer Upgrades in the device, from zero to {@link #MAX_UPGRADES}
     * @return what it holds with them, never above {@link Long#MAX_VALUE}
     */
    public static long scaled(final long base, final int upgrades) {
        if (upgrades < 0 || upgrades > MAX_UPGRADES) {
            throw new IllegalArgumentException("Buffer Upgrades must be from 0 to " + MAX_UPGRADES + ": " + upgrades);
        }
        return SaturatedMath.multiply(base, 1L + upgrades);
    }
}
