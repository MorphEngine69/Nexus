package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.network.DeviceEnergyUse;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.level.DeviceEnergyRow;
import com.morphengine.nexus.level.NetworkEnergyReport;
import com.morphengine.nexus.menu.NexusMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The energy tab of the Nexus panel: the devices of the network with what each draws, supplies and pays in fees,
 * sortable by any column and narrowed to one kind of device. The list scrolls with the wheel and with a scrollbar
 * that can be dragged, as a network may have hundreds of devices. Client thread only.
 */
final class EnergyPanel {

    private static final int ROWS = EnergyLayout.ROWS;
    private static final int TICKS_PER_SECOND = 20;
    private static final int TEXT_INSET = 3;
    private static final int ROW_INSET = 2;
    private static final int MIN_THUMB = 10;
    private static final int ACTIVE_HEADER_FILL = 0x40FFFFFF;
    private static final int ALL_ROLES = -1;
    private static final int NOTHING_SHOWN = -2;

    private final NexusMenu menu;
    private final Font font;
    private EnergySort sort = EnergySort.DRAWN;
    private int role = ALL_ROLES;
    private int scroll;
    private boolean isDragging;
    private @Nullable NetworkEnergyReport shownReport;
    private @Nullable EnergySort shownSort;
    private int shownRole = NOTHING_SHOWN;
    private List<DeviceEnergyRow> rows = List.of();

    EnergyPanel(final NexusMenu menu, final Font font) {
        this.menu = menu;
        this.font = font;
    }

    void draw(final GuiGraphics graphics, final PanelStyle style, final EnergyLayout layout) {
        refresh();
        drawSummary(graphics, style, layout);
        for (EnergySort column : EnergySort.values()) {
            drawHeader(graphics, style, layout.header(column), column);
        }
        drawList(graphics, style, layout);
        drawFooter(graphics, layout);
    }

    boolean click(final EnergyLayout layout, final double x, final double y) {
        if (layout.role().contains(x, y)) {
            role = role + 1 >= DeviceRole.values().length ? ALL_ROLES : role + 1;
            scroll = 0;
            return true;
        }
        for (EnergySort column : EnergySort.values()) {
            if (layout.header(column).contains(x, y)) {
                sort = column;
                scroll = 0;
                return true;
            }
        }
        if (isScrollable() && layout.scrollbar().contains(x, y)) {
            isDragging = true;
            dragTo(layout, y);
            return true;
        }
        return false;
    }

    /**
     * @return whether the scrollbar is being dragged, and moved along with the cursor
     */
    boolean drag(final EnergyLayout layout, final double y) {
        if (isDragging) {
            dragTo(layout, y);
        }
        return isDragging;
    }

    void release() {
        isDragging = false;
    }

    boolean scroll(final EnergyLayout layout, final double x, final double y, final double amount) {
        if (!layout.list().contains(x, y)) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(amount)));
        return true;
    }

    List<Component> tooltip(final EnergyLayout layout, final double x, final double y) {
        if (layout.role().contains(x, y)) {
            return List.of(Component.translatable("gui.nexus.energy.role.tip"));
        }
        for (EnergySort column : EnergySort.values()) {
            if (layout.header(column).contains(x, y)) {
                return List.of(column.label(), column.description().withColor(PanelStyle.TEXT_DIM));
            }
        }
        for (int index = 0; index < Math.min(ROWS, rows.size() - scroll); index++) {
            if (layout.row(index).contains(x, y)) {
                return rowTooltip(rows.get(scroll + index));
            }
        }
        return List.of();
    }

    private static List<Component> rowTooltip(final DeviceEnergyRow row) {
        final BlockPos pos = row.position().pos();
        final DeviceEnergyUse use = row.use();
        return List.of(row.name(),
                Component.translatable("gui.nexus.energy.role." + row.role().name().toLowerCase(Locale.ROOT))
                        .withColor(PanelStyle.TEXT_DIM),
                Component.translatable("gui.nexus.energy.tip.position", pos.getX(), pos.getY(), pos.getZ(),
                        row.position().dimension().location().toString()).withColor(PanelStyle.TEXT_DIM),
                Component.translatable("gui.nexus.energy.tip.drawn", EnergyFormat.exactPerTick(use.drawn())),
                Component.translatable("gui.nexus.energy.tip.supplied", EnergyFormat.exactPerTick(use.supplied())),
                Component.translatable("gui.nexus.energy.tip.tolls", EnergyFormat.exactPerTick(use.tolls())));
    }

    private boolean isScrollable() {
        return rows.size() > ROWS;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - ROWS);
    }

    /**
     * Puts the list where the cursor holds the scrollbar's thumb by its middle.
     */
    private void dragTo(final EnergyLayout layout, final double y) {
        final PanelBounds track = layout.scrollbar();
        final int thumb = thumbHeight(track);
        final double fraction = (y - track.top() - thumb / 2.0) / Math.max(1, track.height() - thumb);
        scroll = (int) Math.round(Math.max(0, Math.min(1, fraction)) * maxScroll());
    }

    private int thumbHeight(final PanelBounds track) {
        return Math.max(MIN_THUMB, track.height() * ROWS / Math.max(ROWS, rows.size()));
    }

    /**
     * Sorts and narrows the list again when the report, the order or the kind of device changed.
     */
    private void refresh() {
        final NetworkEnergyReport report = menu.energy();
        if (report == shownReport && sort == shownSort && role == shownRole) {
            return;
        }
        shownReport = report;
        shownSort = sort;
        shownRole = role;
        final List<DeviceEnergyRow> narrowed = new ArrayList<>(report.devices().size());
        for (DeviceEnergyRow row : report.devices()) {
            if (role == ALL_ROLES || row.role() == DeviceRole.values()[role]) {
                narrowed.add(row);
            }
        }
        narrowed.sort(sort.comparator());
        rows = narrowed;
        scroll = Math.min(scroll, maxScroll());
    }

    private void drawSummary(final GuiGraphics graphics, final PanelStyle style, final EnergyLayout layout) {
        final NetworkStatistics pool = menu.statistics();
        graphics.drawWordWrap(font, Component.translatable("gui.nexus.energy.summary",
                EnergyFormat.perTick(pool.energyOutput() * TICKS_PER_SECOND),
                EnergyFormat.perTick(pool.energyInput() * TICKS_PER_SECOND)), layout.left(),
                layout.summaryY(), layout.width(), PanelStyle.TEXT_LIGHT);
        style.drawButton(graphics, font, layout.role(), role == ALL_ROLES
                ? Component.translatable("gui.nexus.energy.role.all")
                : Component.translatable("gui.nexus.energy.role." + DeviceRole.values()[role].name()
                        .toLowerCase(Locale.ROOT)));
    }

    private void drawHeader(
            final GuiGraphics graphics, final PanelStyle style, final PanelBounds bounds,
            final EnergySort column) {
        style.drawButton(graphics, font, bounds, column.label());
        if (column == sort) {
            graphics.fill(bounds.left(), bounds.top(), bounds.left() + bounds.width(), bounds.top() + bounds.height(),
                    ACTIVE_HEADER_FILL);
        }
    }

    private void drawList(final GuiGraphics graphics, final PanelStyle style, final EnergyLayout layout) {
        final PanelBounds list = layout.list();
        graphics.fill(list.left(), list.top(), list.left() + list.width(), list.top() + list.height(), style.track());
        graphics.renderOutline(list.left(), list.top(), list.width(), list.height(), style.border());
        if (rows.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nexus.energy.empty"), list.left() + TEXT_INSET,
                    list.top() + TEXT_INSET, PanelStyle.TEXT_DIM, false);
            return;
        }
        for (int index = 0; index < Math.min(ROWS, rows.size() - scroll); index++) {
            drawRow(graphics, layout, layout.row(index).top(), rows.get(scroll + index));
        }
        if (isScrollable()) {
            drawScrollbar(graphics, style, layout.scrollbar());
        }
    }

    private void drawScrollbar(final GuiGraphics graphics, final PanelStyle style, final PanelBounds track) {
        graphics.fill(track.left(), track.top(), track.left() + track.width(), track.top() + track.height(),
                style.buttonFill());
        final int thumb = thumbHeight(track);
        final int top = track.top() + (track.height() - thumb) * scroll / Math.max(1, maxScroll());
        graphics.fill(track.left(), top, track.left() + track.width(), top + thumb, style.accent());
    }

    private void drawRow(
            final GuiGraphics graphics, final EnergyLayout layout, final int y, final DeviceEnergyRow row) {
        final PanelBounds name = layout.column(EnergySort.NAME, y, 0);
        graphics.drawString(font, font.plainSubstrByWidth(row.name().getString(), name.width() - ROW_INSET * 2),
                name.left() + ROW_INSET, y + ROW_INSET, PanelStyle.TEXT_LIGHT, false);
        drawFigure(graphics, layout.column(EnergySort.DRAWN, y, 0), row.use().drawn());
        drawFigure(graphics, layout.column(EnergySort.SUPPLIED, y, 0), row.use().supplied());
        drawFigure(graphics, layout.column(EnergySort.TOLLS, y, 0), row.use().tolls());
    }

    private void drawFigure(final GuiGraphics graphics, final PanelBounds column, final long perSecond) {
        final String text = EnergyFormat.perTick(perSecond);
        graphics.drawString(font, text, column.left() + column.width() - font.width(text) - ROW_INSET, column.top()
                + ROW_INSET, perSecond > 0 ? PanelStyle.TEXT_LIGHT : PanelStyle.TEXT_DIM, false);
    }

    private void drawFooter(final GuiGraphics graphics, final EnergyLayout layout) {
        final DeviceEnergyUse portable = menu.energy().portableTerminals();
        graphics.drawString(font, Component.translatable("gui.nexus.energy.portable",
                EnergyFormat.perTick(portable.tolls())), layout.left(), layout.footerY(), PanelStyle.TEXT_DIM, false);
        if (isScrollable()) {
            final String position = (scroll + 1) + "-" + Math.min(rows.size(), scroll + ROWS) + " / " + rows.size();
            graphics.drawString(font, position, layout.right() - font.width(position), layout.footerY(),
                    PanelStyle.TEXT_DIM, false);
        }
    }
}
