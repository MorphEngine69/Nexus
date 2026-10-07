package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.TaskStatus;

import java.util.List;
import java.util.Objects;

/**
 * What a crafting monitor draws of the network's tasks: a row for each of the first few, with a bar of segments lit as
 * far as the task has got. A whole screen of rows packs into one {@code int}, so that it is cheap to send to clients.
 */
public final class TaskRows {

    public static final int ROWS = 3;
    public static final int SEGMENTS = 5;

    private static final int BITS = 3;
    private static final int MASK = (1 << BITS) - 1;
    private static final int NO_TASK = 0;

    private TaskRows() {
    }

    /**
     * @param tasks every task of the network, in the order the monitor lists them; only the first {@link #ROWS} count
     * @return the rows of the monitor, to read back with {@link #hasTask} and {@link #segmentsOf}
     */
    public static int pack(final List<TaskStatus> tasks) {
        Objects.requireNonNull(tasks, "tasks must not be null");
        int packed = 0;
        for (int row = 0; row < Math.min(ROWS, tasks.size()); row++) {
            packed |= (1 + segmentsFor(tasks.get(row).progress())) << (row * BITS);
        }
        return packed;
    }

    public static boolean hasTask(final int packed, final int row) {
        return cellOf(packed, row) != NO_TASK;
    }

    /**
     * @return the segments to light in the bar of {@code row}, from {@code 0} to {@link #SEGMENTS}; {@code 0} too
     *         for a row with no task, which {@link #hasTask} tells apart
     */
    public static int segmentsOf(final int packed, final int row) {
        return Math.max(0, cellOf(packed, row) - 1);
    }

    private static int cellOf(final int packed, final int row) {
        if (row < 0 || row >= ROWS) {
            throw new IllegalArgumentException("a monitor has " + ROWS + " rows, not row " + row);
        }
        return packed >> (row * BITS) & MASK;
    }

    private static int segmentsFor(final double progress) {
        if (progress <= 0) {
            return 0;
        }
        return Math.min(SEGMENTS, (int) Math.ceil(progress * SEGMENTS));
    }
}
