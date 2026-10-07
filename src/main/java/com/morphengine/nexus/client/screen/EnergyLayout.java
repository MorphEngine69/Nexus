package com.morphengine.nexus.client.screen;

/**
 * Where the parts of the energy tab go in a Nexus panel of the given place and width, top to bottom: the summary, the
 * kind of device shown, the column headers that sort, the list with its scrollbar, and the footer.
 *
 * @param left  left edge of the content, inside the panel's padding
 * @param top   top edge of the panel
 * @param width width of the content
 */
record EnergyLayout(int left, int top, int width) {

    /** Rows of the list shown at once. */
    static final int ROWS = 11;

    private static final int PADDING = 8;
    private static final int SUMMARY_Y = 24;
    private static final int ROLE_Y = 44;
    private static final int HEADER_Y = 62;
    private static final int LIST_Y = 78;
    private static final int ROW_HEIGHT = 12;
    private static final int FOOTER_Y = 218;
    private static final int BUTTON_HEIGHT = 14;
    private static final int ROLE_WIDTH = 110;
    private static final int COLUMN_WIDTH = 46;
    private static final int COLUMNS = 3;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;

    static EnergyLayout of(final int panelLeft, final int panelTop, final int panelWidth) {
        return new EnergyLayout(panelLeft + PADDING, panelTop, panelWidth - 2 * PADDING);
    }

    int right() {
        return left + width;
    }

    int summaryY() {
        return top + SUMMARY_Y;
    }

    int footerY() {
        return top + FOOTER_Y;
    }

    PanelBounds role() {
        return new PanelBounds(left, top + ROLE_Y, ROLE_WIDTH, BUTTON_HEIGHT);
    }

    /**
     * @return the width the columns share: the list without its scrollbar
     */
    private int columnsWidth() {
        return width - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
    }

    private int nameWidth() {
        return columnsWidth() - COLUMNS * COLUMN_WIDTH;
    }

    /**
     * @return the column of {@code sort}: the name first, then the three figures
     */
    PanelBounds column(final EnergySort sort, final int y, final int height) {
        final int index = sort.ordinal();
        if (index == 0) {
            return new PanelBounds(left, y, nameWidth(), height);
        }
        return new PanelBounds(left + nameWidth() + (index - 1) * COLUMN_WIDTH, y, COLUMN_WIDTH, height);
    }

    PanelBounds header(final EnergySort sort) {
        return column(sort, top + HEADER_Y, BUTTON_HEIGHT);
    }

    PanelBounds list() {
        return new PanelBounds(left, top + LIST_Y, width, ROWS * ROW_HEIGHT + 2);
    }

    PanelBounds row(final int index) {
        return new PanelBounds(left + 1, top + LIST_Y + 1 + index * ROW_HEIGHT, columnsWidth() - 1, ROW_HEIGHT);
    }

    /**
     * @return the track the scrollbar slides in, along the right edge of the list
     */
    PanelBounds scrollbar() {
        final PanelBounds list = list();
        return new PanelBounds(list.left() + list.width() - SCROLLBAR_WIDTH - 1, list.top() + 1, SCROLLBAR_WIDTH,
                list.height() - 2);
    }
}
