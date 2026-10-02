package com.morphengine.nexus.client.screen;

/**
 * Where the buttons of the crafting request window sit: the row of amount
 * steps under the amount, and Cancel, Craft Less and Start along the bottom.
 *
 * @param panel the window
 */
record CraftRequestButtons(PanelBounds panel) {

    static final int HEIGHT = 14;
    static final int STEPS_TOP = 42;
    private static final int BOTTOM_MARGIN = 22;
    private static final int ACTION_WIDTH = 60;
    private static final int CRAFT_LESS_WIDTH = 92;
    private static final int STEP_WIDTH = 30;
    private static final int STEP_GAP = 2;

    /**
     * @param index index of the step in a row of {@code count} steps
     */
    PanelBounds step(final int index, final int count) {
        final int rowWidth = count * (STEP_WIDTH + STEP_GAP) - STEP_GAP;
        return new PanelBounds(panel.left() + (panel.width() - rowWidth) / 2 + index * (STEP_WIDTH + STEP_GAP),
                panel.top() + STEPS_TOP, STEP_WIDTH, HEIGHT);
    }

    PanelBounds cancel() {
        return new PanelBounds(panel.left() + PanelStyle.PADDING, bottomTop(), ACTION_WIDTH, HEIGHT);
    }

    PanelBounds craftLess() {
        return new PanelBounds(panel.left() + (panel.width() - CRAFT_LESS_WIDTH) / 2, bottomTop(), CRAFT_LESS_WIDTH,
                HEIGHT);
    }

    PanelBounds start() {
        return new PanelBounds(panel.left() + panel.width() - PanelStyle.PADDING - ACTION_WIDTH, bottomTop(),
                ACTION_WIDTH, HEIGHT);
    }

    private int bottomTop() {
        return panel.top() + panel.height() - BOTTOM_MARGIN;
    }
}
