package com.morphengine.nexus.terminal;

/**
 * Where things sit in a terminal panel, in pixels from its top left corner.
 * The screen picks the layout that fits the window and hands it to the menu,
 * which moves its slots there. From the top: header, search box, the grid of
 * resources with its scroll bar, the crafting grid of a crafting terminal, the
 * inventory. The crafting grid or the encoder and the inventory are centred
 * under the grid.
 *
 * @param columns columns of resources the grid shows, at least {@value #MIN_COLUMNS}
 * @param rows    rows of resources the grid shows, at least one
 */
public record TerminalLayout(TerminalKind kind, int columns, int rows) {

    public static final int MIN_COLUMNS = 9;
    public static final int SLOT = 18;
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 36;
    public static final int SEARCH_LEFT = 8;
    public static final int SEARCH_TOP = 20;
    public static final int SEARCH_HEIGHT = 12;
    public static final int SCROLLBAR_WIDTH = 6;
    /** Room left of the panel for the column of buttons beside it. */
    public static final int SIDEBAR_ROOM = 24;

    private static final int SCROLLBAR_GAP = 4;
    private static final int RIGHT_PADDING = 8;
    private static final int GRID_GAP = 4;
    private static final int INVENTORY_GAP = 6;
    private static final int CRAFTING_HEIGHT = 3 * SLOT;
    private static final int RESULT_OFFSET = 3 * SLOT + 36;
    private static final int INVENTORY_HEIGHT = 4 * SLOT + 4;
    private static final int BOTTOM_PADDING = 7;
    private static final int WINDOW_MARGIN = 4;

    public TerminalLayout {
        if (columns < MIN_COLUMNS) {
            throw new IllegalArgumentException("a terminal shows at least " + MIN_COLUMNS + " columns: " + columns);
        }
        if (rows <= 0) {
            throw new IllegalArgumentException("a terminal shows at least one row: " + rows);
        }
    }

    /**
     * @return the layout of {@code size}, shrunk to what fits a window of the
     *         given size in GUI pixels
     */
    public static TerminalLayout fit(
            final TerminalKind kind, final TerminalSize size, final int windowWidth, final int windowHeight) {
        final int widthForGrid = windowWidth - 2 * SIDEBAR_ROOM - chromeWidth();
        final int columns = Math.max(MIN_COLUMNS, Math.min(size.columns(), widthForGrid / SLOT));
        final int heightForGrid = windowHeight - 2 * WINDOW_MARGIN - new TerminalLayout(kind, columns, 1).height()
                + SLOT;
        final int rows = Math.max(1, Math.min(kind.rows(size), heightForGrid / SLOT));
        return new TerminalLayout(kind, columns, rows);
    }

    private static int chromeWidth() {
        return GRID_LEFT + SCROLLBAR_GAP + SCROLLBAR_WIDTH + RIGHT_PADDING;
    }

    public int width() {
        return columns * SLOT + chromeWidth();
    }

    public int height() {
        return inventoryTop() + INVENTORY_HEIGHT + BOTTOM_PADDING;
    }

    public int scrollbarLeft() {
        return GRID_LEFT + columns * SLOT + SCROLLBAR_GAP;
    }

    public int gridBottom() {
        return GRID_TOP + rows * SLOT;
    }

    /**
     * @return left edge of the inventory, centred under the grid
     */
    public int inventoryLeft() {
        return GRID_LEFT + (columns - MIN_COLUMNS) * SLOT / 2;
    }

    public int inventoryTop() {
        return kind.hasWorkArea()
                ? craftingTop() + CRAFTING_HEIGHT + GRID_GAP
                : gridBottom() + INVENTORY_GAP;
    }

    /**
     * @return left edge of the encoder's area, as wide as the inventory; meaningful
     *         for a blueprint terminal only
     */
    public int encoderLeft() {
        return inventoryLeft();
    }

    /**
     * @return left edge of the crafting grid; meaningful for a crafting terminal only
     */
    public int craftingLeft() {
        return inventoryLeft() + SLOT;
    }

    /**
     * @return top of the crafting grid or the encoder's area
     */
    public int craftingTop() {
        return gridBottom() + GRID_GAP;
    }

    public int craftingRight() {
        return craftingLeft() + CRAFTING_HEIGHT;
    }

    public int resultLeft() {
        return craftingLeft() + RESULT_OFFSET;
    }

    public int resultTop() {
        return craftingTop() + SLOT;
    }
}
