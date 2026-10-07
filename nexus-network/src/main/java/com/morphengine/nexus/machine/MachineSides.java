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
     * @return both on every side, for a device that took anything from any side before its sides could be chosen
     */
    public static SideConfig<MachineSide> allOpen() {
        SideConfig<MachineSide> open = SideConfig.closed(MachineSide.class);
        for (MachineSide side : MachineSide.values()) {
            open = open.with(side, SideMode.BOTH);
        }
        return open;
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
