package com.morphengine.nexus.client.render;

import com.morphengine.nexus.block.EnergyCellBlock;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * The bar of an Energy Cell: eight segments, which of them show and how wide, by the {@link SegmentSweep} schedule
 * while the cell charges.
 */
final class ChargeBar {

    static final String SEGMENT_PREFIX = "seg_";

    private static final List<Direction> SIDES = List.of(
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    private ChargeBar() {
    }

    /**
     * @param index    the segment, counted from the left, from 0
     * @param level    how many segments the charge lights
     * @param charging whether energy is coming in
     * @param ticks    the time, in ticks, never negative
     * @return how much of the segment shows, from 0 (none) to 1 (whole)
     */
    static float fill(final int index, final int level, final boolean charging, final float ticks) {
        return SegmentSweep.QUICK.fill(index, level, EnergyCellBlock.SEGMENTS, charging, ticks);
    }

    /**
     * @return the names of the segment bones of the bar on every side
     */
    static List<String> segmentBones() {
        final List<String> names = new ArrayList<>();
        for (Direction side : SIDES) {
            for (int number = 1; number <= EnergyCellBlock.SEGMENTS; number++) {
                names.add(boneName(number, side));
            }
        }
        return List.copyOf(names);
    }

    static String boneName(final int number, final Direction side) {
        return SEGMENT_PREFIX + number + "_" + side.getName();
    }

    /**
     * @return whether the bar of {@code side} runs along the z axis of the model, rather than the x axis
     */
    static boolean runsAlongZ(final Direction side) {
        return side.getAxis() == Direction.Axis.X;
    }

    static List<Direction> sides() {
        return SIDES;
    }
}
