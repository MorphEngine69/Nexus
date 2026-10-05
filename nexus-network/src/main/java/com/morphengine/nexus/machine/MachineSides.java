package com.morphengine.nexus.machine;

import com.morphengine.nexus.transport.SideConfig;
import com.morphengine.nexus.transport.SideMode;

/**
 * The settings of the sides of a new machine.
 */
public final class MachineSides {

    private MachineSides() {
    }

    /**
     * @return input from the top, the front and the left, output to the bottom and the right, both at the back
     */
    public static SideConfig<MachineSide> defaults() {
        return SideConfig.closed(MachineSide.class)
                .with(MachineSide.TOP, SideMode.INPUT)
                .with(MachineSide.BOTTOM, SideMode.OUTPUT)
                .with(MachineSide.FRONT, SideMode.INPUT)
                .with(MachineSide.BACK, SideMode.BOTH)
                .with(MachineSide.LEFT, SideMode.INPUT)
                .with(MachineSide.RIGHT, SideMode.OUTPUT);
    }
}
