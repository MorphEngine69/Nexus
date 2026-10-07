package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.machine.MachineSide;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * How the sides of a block are unfolded into the squares of a {@link SideModePanel}: a cross of four squares in a row,
 * with one above and one below the second of them. A cell is {@code {column, row}}.
 */
final class SideLayouts {

    /** The six faces of a block, a cell for each. */
    static final Map<Direction, int[]> CUBE = cube();
    /** The six sides of a machine counted from its front, which stands in the middle of the cross. */
    static final Map<MachineSide, int[]> MACHINE = machine();

    private static final int FAR_COLUMN = 3;

    private SideLayouts() {
    }

    static String cubeName(final Direction side) {
        return side.getName();
    }

    static String machineName(final MachineSide side) {
        return side.name().toLowerCase(Locale.ROOT);
    }

    static Function<Direction, String> cubeNames() {
        return SideLayouts::cubeName;
    }

    static Function<MachineSide, String> machineNames() {
        return SideLayouts::machineName;
    }

    private static Map<Direction, int[]> cube() {
        final Map<Direction, int[]> cells = new EnumMap<>(Direction.class);
        cells.put(Direction.UP, new int[] {1, 0});
        cells.put(Direction.WEST, new int[] {0, 1});
        cells.put(Direction.NORTH, new int[] {1, 1});
        cells.put(Direction.EAST, new int[] {2, 1});
        cells.put(Direction.SOUTH, new int[] {FAR_COLUMN, 1});
        cells.put(Direction.DOWN, new int[] {1, 2});
        return Map.copyOf(cells);
    }

    private static Map<MachineSide, int[]> machine() {
        final Map<MachineSide, int[]> cells = new EnumMap<>(MachineSide.class);
        cells.put(MachineSide.TOP, new int[] {1, 0});
        cells.put(MachineSide.LEFT, new int[] {0, 1});
        cells.put(MachineSide.FRONT, new int[] {1, 1});
        cells.put(MachineSide.RIGHT, new int[] {2, 1});
        cells.put(MachineSide.BACK, new int[] {FAR_COLUMN, 1});
        cells.put(MachineSide.BOTTOM, new int[] {1, 2});
        return Map.copyOf(cells);
    }
}
