package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.menu.TerminalPanel;
import com.morphengine.nexus.networking.TerminalClickPayload;
import com.morphengine.nexus.networking.TerminalSettingsPayload;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.terminal.GridClick;
import com.morphengine.nexus.terminal.TerminalLayout;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.terminal.TerminalStatus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Panel of a Terminal or Crafting Terminal, tinted with the network's color:
 * a search box, the grid of the network's resources with the sort and view
 * buttons beside the panel, a crafting grid for the crafting terminal, and the
 * inventory. Clicks on the grid go to the server, which answers with the
 * changed contents.
 *
 * @param <M> the terminal's menu
 */
public final class TerminalScreen<M extends AbstractContainerMenu & TerminalPanel> extends PanelScreen<M> {

    private static final int CLEAR_SIZE = 10;
    private static final int CLEAR_GAP = 2;
    private static final int ARROW_LENGTH = 22;
    private static final int ARROW_GAP = 6;
    private static final int ARROW_HEAD = 4;

    private final ResourceGridView view = new ResourceGridView();
    private TerminalLayout layout;
    private TerminalSettings settings;
    private @Nullable TerminalSearch search;
    private @Nullable ResourceGrid grid;
    private @Nullable TerminalSidebar sidebar;

    public TerminalScreen(final M menu, final Inventory inventory, final Component title) {
        this(menu, inventory, title, TerminalLayout.fit(
                menu.terminal().kind(), menu.terminal().settings().size(), 0, 0));
    }

    private TerminalScreen(final M menu, final Inventory inventory, final Component title,
                           final TerminalLayout initial) {
        super(menu, inventory, title, initial.width(), initial.height());
        this.layout = initial;
        this.settings = menu.terminal().settings();
    }

    /**
     * Picks the layout of the chosen size that fits the window, then moves the
     * slots and the panel there. Runs on opening, on a resize of the window and
     * when the player picks another size.
     */
    @Override
    protected void init() {
        layout = TerminalLayout.fit(getMenu().terminal().kind(), settings.size(), width, height);
        imageWidth = layout.width();
        imageHeight = layout.height();
        getMenu().layOut(layout);
        super.init();
        grid = new ResourceGrid(leftPos, topPos, layout);
        sidebar = new TerminalSidebar(leftPos, topPos);
        search = new TerminalSearch(font, new PanelBounds(leftPos + TerminalLayout.SEARCH_LEFT,
                topPos + TerminalLayout.SEARCH_TOP, searchWidth(), TerminalLayout.SEARCH_HEIGHT),
                search != null ? search.text() : "");
        addRenderableWidget(search.widget());
    }

    /**
     * Coming back from another screen, such as a recipe viewer's, the player
     * has found what was searched for: the search starts empty again. A resize
     * keeps it, since it only runs {@link #init}.
     */
    @Override
    public void added() {
        super.added();
        if (search != null) {
            search.clear();
        }
    }

    @Override
    protected boolean isTitleEditable() {
        return false;
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().badge());
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        if (search != null) {
            search.draw(graphics, style, mouseX, mouseY);
        }
        if (grid != null) {
            grid.show(view.update(getMenu().terminal().contents(), settings, search != null ? search.text() : ""));
            grid.draw(graphics, font, style, mouseX, mouseY);
        }
        drawStatus(graphics);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        if (layout.kind().hasCraftingGrid()) {
            drawCraftingExtras(graphics, style, mouseX, mouseY);
        }
        if (sidebar != null) {
            sidebar.draw(graphics, style, settings, mouseX, mouseY);
        }
    }

    private int searchWidth() {
        return layout.scrollbarLeft() + TerminalLayout.SCROLLBAR_WIDTH - TerminalLayout.SEARCH_LEFT;
    }

    private void drawStatus(final GuiGraphicsExtractor graphics) {
        final TerminalStatus status = getMenu().terminal().contents().status();
        if (status == TerminalStatus.ONLINE) {
            return;
        }
        final Component message = Component.translatable(status == TerminalStatus.NO_ENERGY
                ? "gui.nexus.terminal.no_energy" : "gui.nexus.terminal.no_network");
        final int centerX = leftPos + TerminalLayout.GRID_LEFT + layout.columns() * TerminalLayout.SLOT / 2;
        final int centerY = topPos + (TerminalLayout.GRID_TOP + layout.gridBottom() - font.lineHeight) / 2;
        graphics.centeredText(font, message, centerX, centerY, PanelStyle.TEXT_LIGHT);
    }

    private void drawCraftingExtras(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        drawArrow(graphics, style.border());
        style.drawCrossButton(graphics, clearButton(), clearButton().contains(mouseX, mouseY));
    }

    /**
     * A two pixel shaft and a head that narrows to the tip, pointing at the result.
     */
    private void drawArrow(final GuiGraphicsExtractor graphics, final int color) {
        final int left = leftPos + layout.craftingRight() + ARROW_GAP;
        final int y = topPos + layout.resultTop() + TerminalLayout.SLOT / 2 - 2;
        final int headLeft = left + ARROW_LENGTH - ARROW_HEAD;
        graphics.fill(left, y, headLeft, y + 2, color);
        for (int step = 0; step < ARROW_HEAD; step++) {
            final int half = ARROW_HEAD - 1 - step;
            graphics.fill(headLeft + step, y - half, headLeft + step + 1, y + 2 + half, color);
        }
    }

    /**
     * A small square beside the top right corner of the crafting grid, as in
     * other storage mods.
     */
    private PanelBounds clearButton() {
        return new PanelBounds(leftPos + layout.craftingRight() + CLEAR_GAP, topPos + layout.craftingTop() - 1,
                CLEAR_SIZE, CLEAR_SIZE);
    }

    /**
     * @return where the buttons beside the panel are, for a recipe viewer to keep clear
     */
    public Rect2i sidebarArea() {
        return sidebar != null ? sidebar.area().toRect() : new Rect2i(leftPos, topPos, 0, 0);
    }

    /**
     * @return the resource of the grid under the cursor with the area of its
     *         icon; {@code null} over an empty cell or outside the grid
     */
    public @Nullable HoveredResource hoveredResource(final double mouseX, final double mouseY) {
        final NexusResource resource = grid != null ? grid.resourceAt(mouseX, mouseY) : null;
        final PanelBounds cell = grid != null ? grid.hoveredCell(mouseX, mouseY) : null;
        return resource != null && cell != null ? new HoveredResource(resource, cell.toRect()) : null;
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        final List<Component> lines = tooltipAt(mouseX, mouseY);
        if (!lines.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private List<Component> tooltipAt(final int mouseX, final int mouseY) {
        if (layout.kind().hasCraftingGrid() && clearButton().contains(mouseX, mouseY)) {
            return List.of(Component.translatable("gui.nexus.terminal.clear_grid"));
        }
        if (search != null && search.isOverReset(mouseX, mouseY)) {
            return List.of(Component.translatable("gui.nexus.terminal.clear_search"));
        }
        if (sidebar != null && sidebar.contains(mouseX, mouseY)) {
            return sidebar.tooltip(settings, mouseX, mouseY);
        }
        return grid != null && getMenu().getCarried().isEmpty() ? grid.tooltip(mouseX, mouseY) : List.of();
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (search != null && search.click(event.x(), event.y(), isSecondary(event))) {
            return true;
        }
        if (search != null && getFocused() == search.widget() && !search.widget().isFocused()) {
            setFocused(null);
        }
        if (clickSidebar(event) || clickGrid(event)) {
            return true;
        }
        return clickClearButton(event) || super.mouseClicked(event, doubleClick);
    }

    private boolean clickSidebar(final MouseButtonEvent event) {
        final TerminalSettings changed = sidebar != null
                ? sidebar.click(settings, event.x(), event.y(), isSecondary(event)) : null;
        if (changed != null) {
            applySettings(changed);
        }
        return changed != null;
    }

    private boolean clickGrid(final MouseButtonEvent event) {
        if (grid == null || grid.startDrag(event.x(), event.y())) {
            return grid != null;
        }
        if (!grid.containsCells(event.x(), event.y())) {
            return false;
        }
        final NexusResource resource = grid.resourceAt(event.x(), event.y());
        final GridClick click = event.hasShiftDown() ? GridClick.QUICK_MOVE
                : isSecondary(event) ? GridClick.SECONDARY : GridClick.PRIMARY;
        if (click != GridClick.QUICK_MOVE || resource != null) {
            ClientPacketDistributor.sendToServer(new TerminalClickPayload(getMenu().containerId, resource, click));
        }
        return true;
    }

    private boolean clickClearButton(final MouseButtonEvent event) {
        if (!layout.kind().hasCraftingGrid() || !clearButton().contains(event.x(), event.y())
                || minecraft == null || minecraft.gameMode == null) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, CraftingTerminalMenu.BUTTON_CLEAR_GRID);
        return true;
    }

    private static boolean isSecondary(final MouseButtonEvent event) {
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    /**
     * Shows the new settings at once and lets the terminal keep them.
     */
    private void applySettings(final TerminalSettings changed) {
        final boolean resized = changed.size() != settings.size();
        settings = changed;
        ClientPacketDistributor.sendToServer(new TerminalSettingsPayload(getMenu().containerId, changed));
        if (resized) {
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        return grid != null && grid.drag(event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (grid != null) {
            grid.stopDrag();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        return grid != null && grid.scroll(x, y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return search != null && search.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    protected boolean hasClickedOutside(final double mouseX, final double mouseY, final int left, final int top) {
        return super.hasClickedOutside(mouseX, mouseY, left, top)
                && (sidebar == null || !sidebar.contains(mouseX, mouseY));
    }

    /**
     * A resource of the grid under the cursor.
     *
     * @param area the icon's area on the screen
     */
    public record HoveredResource(NexusResource resource, Rect2i area) {
    }
}
