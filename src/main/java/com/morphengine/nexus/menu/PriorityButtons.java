package com.morphengine.nexus.menu;

/**
 * The row of buttons that raise or lower a device's priority by one or by
 * ten, raising first. Each button sends its index, offset by where the menu
 * places the row among its button ids.
 */
public final class PriorityButtons {

    private static final int[] STEPS = {1, 10, -1, -10};

    private PriorityButtons() {
    }

    public static int count() {
        return STEPS.length;
    }

    /**
     * @param index index of the button in the row, from 0 to {@link #count()} - 1
     * @return the change the button makes to the priority
     */
    public static int stepOf(final int index) {
        return STEPS[index];
    }

    /**
     * @param index index of the button in the row, from 0 to {@link #count()} - 1
     * @return the button's label: its step with an explicit sign, such as {@code +10}
     */
    public static String labelOf(final int index) {
        return String.format("%+d", STEPS[index]);
    }
}
