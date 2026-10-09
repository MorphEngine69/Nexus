package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.terminal.TerminalContents;
import com.morphengine.nexus.terminal.TerminalEntry;
import com.morphengine.nexus.terminal.TerminalLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The scrolling grid of a terminal: one cell per resource with its icon and a
 * short amount in the corner, and a scroll bar on the right. What a cell needs
 * to draw is prepared once per list, for the cells shown, so a full-screen grid
 * redraws cheaply every frame. Client thread only.
 */
final class ResourceGrid {

    private static final int ICON_INSET = 1;
    private static final int ICON_SIZE = 16;
    private static final int HOVER_RGB = 0x80FFFFFF;
    private static final int MIN_THUMB = 8;

    private final PanelBounds cells;
    private final PanelBounds track;
    private final int columns;
    private final int rows;
    private List<TerminalEntry> entries = List.of();
    private @Nullable ShownCell[] prepared = new ShownCell[0];
    private int scroll;
    private boolean dragging;

    ResourceGrid(final int panelLeft, final int panelTop, final TerminalLayout layout) {
        this.columns = layout.columns();
        this.rows = layout.rows();
        this.cells = new PanelBounds(panelLeft + TerminalLayout.GRID_LEFT, panelTop + TerminalLayout.GRID_TOP,
                columns * TerminalLayout.SLOT, rows * TerminalLayout.SLOT);
        this.track = new PanelBounds(panelLeft + layout.scrollbarLeft(), cells.top(),
                TerminalLayout.SCROLLBAR_WIDTH, cells.height());
    }

    /**
     * @param shown the resources to show; the same list again keeps what was prepared
     */
    void show(final List<TerminalEntry> shown) {
        if (shown != entries) {
            entries = shown;
            prepared = new ShownCell[shown.size()];
        }
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    boolean containsCells(final double x, final double y) {
        return cells.contains(x, y);
    }

    void draw(final GuiGraphics graphics, final Font font, final PanelStyle style,
              final int mouseX, final int mouseY) {
        style.drawSlotGrid(graphics, cells.left(), cells.top(), columns, rows);
        final int first = scroll * columns;
        final int last = Math.min(entries.size(), first + rows * columns);
        for (int index = first; index < last; index++) {
            cellAt(index).icon().draw(graphics, cellLeft(index - first), cellTop(index - first));
        }
        for (int index = first; index < last; index++) {
            SlotAmounts.draw(graphics, font, cellAt(index).amount(), cellLeft(index - first), cellTop(index - first));
        }
        final PanelBounds hovered = hoveredCell(mouseX, mouseY);
        if (hovered != null) {
            graphics.fill(hovered.left(), hovered.top(), hovered.left() + ICON_SIZE, hovered.top() + ICON_SIZE,
                    HOVER_RGB);
        }
        drawScrollBar(graphics, style);
    }

    private int cellLeft(final int shownIndex) {
        return cells.left() + shownIndex % columns * TerminalLayout.SLOT + ICON_INSET;
    }

    private int cellTop(final int shownIndex) {
        return cells.top() + shownIndex / columns * TerminalLayout.SLOT + ICON_INSET;
    }

    private ShownCell cellAt(final int index) {
        ShownCell cell = prepared[index];
        if (cell == null) {
            final TerminalEntry entry = entries.get(index);
            final String amount = entry.amount() == 0 ? Component.translatable("gui.nexus.terminal.craft").getString()
                    : entry.resource().type().unit().compact(entry.amount());
            cell = new ShownCell(ResourceRenderers.icon(entry.resource()), amount);
            prepared[index] = cell;
        }
        return cell;
    }

    private void drawScrollBar(final GuiGraphics graphics, final PanelStyle style) {
        graphics.fill(track.left(), track.top(), track.left() + track.width(), track.top() + track.height(),
                style.track());
        graphics.renderOutline(track.left(), track.top(), track.width(), track.height(), style.border());
        final int thumbHeight = thumbHeight();
        final int travel = track.height() - 2 - thumbHeight;
        final int thumbTop = track.top() + 1 + (maxScroll() == 0 ? 0 : travel * scroll / maxScroll());
        graphics.fill(track.left() + 1, thumbTop, track.left() + track.width() - 1, thumbTop + thumbHeight,
                maxScroll() == 0 ? style.border() : style.accent());
    }

    /**
     * @return the resource under the cursor; {@code null} over an empty cell or outside the grid
     */
    @Nullable NexusResource resourceAt(final double x, final double y) {
        if (!cells.contains(x, y)) {
            return null;
        }
        final int column = (int) (x - cells.left()) / TerminalLayout.SLOT;
        final int row = (int) (y - cells.top()) / TerminalLayout.SLOT;
        final TerminalEntry entry = entryAtIndex(row * columns + column);
        return entry != null ? entry.resource() : null;
    }

    /**
     * @return the tooltip of the resource under the cursor: its own lines, the
     *         amount held, and for what the network can craft how to ask for it
     */
    List<Component> tooltip(final double x, final double y, final TerminalContents contents) {
        final NexusResource resource = resourceAt(x, y);
        if (resource == null) {
            return List.of();
        }
        final List<Component> lines = new ArrayList<>(ResourceRenderers.tooltip(resource));
        final long amount = amountOf(resource);
        lines.add(Math.min(1, lines.size()), resource.type().unit().exact(amount).withStyle(ChatFormatting.GRAY));
        if (contents.isCraftable(resource)) {
            lines.add(Component.translatable(amount == 0 ? "gui.nexus.terminal.craft_hint"
                    : "gui.nexus.terminal.craft_more_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    boolean scroll(final double x, final double y, final double amount) {
        if (!cells.contains(x, y) && !track.contains(x, y)) {
            return false;
        }
        scroll = Math.clamp(scroll - (long) Math.signum(amount), 0, maxScroll());
        return true;
    }

    boolean startDrag(final double x, final double y) {
        dragging = track.contains(x, y);
        if (dragging) {
            dragTo(y);
        }
        return dragging;
    }

    boolean drag(final double y) {
        if (dragging) {
            dragTo(y);
        }
        return dragging;
    }

    void stopDrag() {
        dragging = false;
    }

    private void dragTo(final double y) {
        final double travel = Math.max(1, track.height() - thumbHeight());
        final double progress = (y - track.top() - thumbHeight() / 2.0) / travel;
        scroll = Math.clamp(Math.round(progress * maxScroll()), 0, maxScroll());
    }

    private int thumbHeight() {
        final int totalRows = Math.max(rows, totalRows());
        return Math.max(MIN_THUMB, (track.height() - 2) * rows / totalRows);
    }

    private int totalRows() {
        return (entries.size() + columns - 1) / columns;
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - rows);
    }

    private long amountOf(final NexusResource resource) {
        for (TerminalEntry entry : entries) {
            if (entry.resource().equals(resource)) {
                return entry.amount();
            }
        }
        return 0;
    }

    private @Nullable TerminalEntry entryAtIndex(final int shownIndex) {
        final int index = scroll * columns + shownIndex;
        return index < entries.size() ? entries.get(index) : null;
    }

    @Nullable PanelBounds hoveredCell(final double x, final double y) {
        if (!cells.contains(x, y)) {
            return null;
        }
        final int column = (int) (x - cells.left()) / TerminalLayout.SLOT;
        final int row = (int) (y - cells.top()) / TerminalLayout.SLOT;
        return new PanelBounds(cells.left() + column * TerminalLayout.SLOT + ICON_INSET,
                cells.top() + row * TerminalLayout.SLOT + ICON_INSET, ICON_SIZE, ICON_SIZE);
    }

    /**
     * What a cell draws, looked up once.
     */
    private record ShownCell(ResourceIcon icon, String amount) {
    }
}
