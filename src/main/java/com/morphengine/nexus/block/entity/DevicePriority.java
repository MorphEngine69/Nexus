package com.morphengine.nexus.block.entity;

/**
 * The range of a device's priority, the same for every device that has one.
 */
public final class DevicePriority {

    public static final int MIN = -9999;
    public static final int MAX = 9999;

    private DevicePriority() {
    }

    /**
     * @return {@code priority} brought into [{@value #MIN}, {@value #MAX}]
     */
    public static int clamp(final int priority) {
        return Math.clamp(priority, MIN, MAX);
    }
}
