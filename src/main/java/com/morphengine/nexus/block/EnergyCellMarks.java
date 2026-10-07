package com.morphengine.nexus.block;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * The marks of the tier under the battery of an Energy Cell: as many squares as its rank, in a bone of the model for
 * each rank and side, of which a cell shows the one of its own rank.
 */
public final class EnergyCellMarks {

    /** The highest rank the model draws marks for. */
    public static final int MAX_RANK = 4;

    private static final List<Direction> SIDES = List.of(
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    private EnergyCellMarks() {
    }

    /**
     * @return the prefix of the bones of the marks of {@code rank}, which the side follows after an underscore
     */
    public static String bonePrefix(final int rank) {
        return "marks_" + rank;
    }

    public static String boneName(final int rank, final Direction side) {
        return bonePrefix(rank) + "_" + side.getName();
    }

    /**
     * @return the names of the bones of the marks of every rank but {@code rank}, on every side
     */
    public static List<String> bonesOfOtherRanks(final int rank) {
        final List<String> names = new ArrayList<>();
        for (int other = 1; other <= MAX_RANK; other++) {
            if (other != rank) {
                for (Direction side : SIDES) {
                    names.add(boneName(other, side));
                }
            }
        }
        return List.copyOf(names);
    }
}
