package com.morphengine.nexus.processing;

import com.morphengine.nexus.machine.MachineSide;
import net.minecraft.core.Direction;

/**
 * Turns the sides of the world into the sides of a machine and back, for a machine that faces one of the four
 * horizontal directions: the front is where it faces, the left is on the left of someone who looks at the front.
 */
public final class MachineFacing {

    private MachineFacing() {
    }

    /**
     * @param facing the way the front of the machine faces
     * @param world  a side of the machine in the world
     */
    public static MachineSide sideOf(final Direction facing, final Direction world) {
        for (MachineSide side : MachineSide.values()) {
            if (worldSide(facing, side) == world) {
                return side;
            }
        }
        throw new IllegalArgumentException("No side of a machine facing " + facing + " is on " + world);
    }

    /**
     * @return the side of the world that {@code side} of a machine facing {@code facing} is on
     */
    public static Direction worldSide(final Direction facing, final MachineSide side) {
        return switch (side) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
        };
    }
}
