package com.morphengine.nexus.client.screen;

/**
 * Where the parts of the access tab go in a Nexus panel of the given place and width.
 *
 * @param left  left edge of the content, inside the panel's padding
 * @param top   top edge of the panel
 * @param width width of the content
 */
record AccessLayout(int left, int top, int width) {

    /** Rows of the list shown at once. */
    static final int ROWS = 10;
    /** Side of the box of a permission. */
    static final int BOX = 9;

    private static final int PADDING = 8;
    private static final int OWNER_Y = 24;
    private static final int DEFAULT_Y = 36;
    private static final int HEADER_Y = 54;
    private static final int LIST_Y = 72;
    private static final int ROW_HEIGHT = 12;
    private static final int DETAIL_Y = 200;
    private static final int CONTROLS_Y = 212;
    private static final int TOGGLES_Y = 232;
    private static final int BUTTON_WIDTH = 72;
    private static final int BUTTON_HEIGHT = 14;
    private static final int ARROW_WIDTH = 10;
    private static final int ROLE_WIDTH = 76;
    private static final int GAP = 6;
    private static final int ACTION_WIDTH = 56;

    static AccessLayout of(final int panelLeft, final int panelTop, final int panelWidth) {
        return new AccessLayout(panelLeft + PADDING, panelTop, panelWidth - 2 * PADDING);
    }

    int right() {
        return left + width;
    }

    int ownerY() {
        return top + OWNER_Y;
    }

    PanelBounds defaultRole() {
        return new PanelBounds(right() - BUTTON_WIDTH, top + DEFAULT_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    PanelBounds header() {
        return new PanelBounds(right() - BUTTON_WIDTH, top + HEADER_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    PanelBounds list() {
        return new PanelBounds(left, top + LIST_Y, width, ROWS * ROW_HEIGHT + 2);
    }

    PanelBounds row(final int index) {
        return new PanelBounds(left + 1, top + LIST_Y + 1 + index * ROW_HEIGHT, width - 2, ROW_HEIGHT);
    }

    int detailY() {
        return top + DETAIL_Y;
    }

    PanelBounds rolePrevious() {
        return new PanelBounds(left, top + CONTROLS_Y, ARROW_WIDTH, BUTTON_HEIGHT);
    }

    PanelBounds roleLabel() {
        return new PanelBounds(left + ARROW_WIDTH, top + CONTROLS_Y, ROLE_WIDTH, BUTTON_HEIGHT);
    }

    PanelBounds roleNext() {
        return new PanelBounds(left + ARROW_WIDTH + ROLE_WIDTH, top + CONTROLS_Y, ARROW_WIDTH, BUTTON_HEIGHT);
    }

    PanelBounds remove() {
        return new PanelBounds(transfer().left() - GAP - ACTION_WIDTH, top + CONTROLS_Y, ACTION_WIDTH,
                BUTTON_HEIGHT);
    }

    PanelBounds transfer() {
        return new PanelBounds(right() - ACTION_WIDTH, top + CONTROLS_Y, ACTION_WIDTH, BUTTON_HEIGHT);
    }

    /**
     * @return the place of permission {@code index}: two columns, filled row by row
     */
    PanelBounds toggle(final int index) {
        final int column = index % 2;
        final int row = index / 2;
        return new PanelBounds(left + column * width / 2, top + TOGGLES_Y + row * BUTTON_HEIGHT, width / 2,
                BUTTON_HEIGHT);
    }
}
