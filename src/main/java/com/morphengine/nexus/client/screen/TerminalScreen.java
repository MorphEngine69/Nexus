package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.client.input.KeyEvent;
import com.morphengine.nexus.client.input.MouseButtonEvent;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.menu.TerminalPanel;
import com.morphengine.nexus.networking.TerminalClickPayload;
import com.morphengine.nexus.networking.TerminalSettingsPayload;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.terminal.GridClick;
import com.morphengine.nexus.terminal.TerminalContents;
import com.morphengine.nexus.terminal.TerminalLayout;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.terminal.TerminalStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * Panel of a terminal, tinted with the network's color: a search box, the
 * grid of the network's resources with the sort and view buttons beside the
 * panel, a crafting grid or a Blueprint encoder for the terminals with one,
 * and the inventory. Clicks on the grid go to the server, which answers with
 * the changed contents. A resource the network can craft opens the crafting
 * request when clicked with none stored, with Ctrl or with the middle button.
 *
 * @param <M> the terminal's menu
 */
public final class TerminalScreen<M extends AbstractContainerMenu & TerminalPanel> extends PanelScreen<M>
        implements FilterScreen, SideAreas {

    private final ResourceGridView view = new ResourceGridView();
    private TerminalLayout layout;
    private TerminalSettings settings;
    private @Nullable TerminalSearch search;
    private @Nullable ResourceGrid grid;
    private @Nullable TerminalSidebar sidebar;
    private @Nullable TerminalWorkArea workArea;

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
        workArea = createWorkArea();
        final int searchWidth = layout.scrollbarLeft() + TerminalLayout.SCROLLBAR_WIDTH - TerminalLayout.SEARCH_LEFT;
        search = new TerminalSearch(font, new PanelBounds(leftPos + TerminalLayout.SEARCH_LEFT,
                topPos + TerminalLayout.SEARCH_TOP, searchWidth, TerminalLayout.SEARCH_HEIGHT),
                search != null ? search.text() : "");
        addRenderableWidget(search.widget());
    }

    private @Nullable TerminalWorkArea createWorkArea() {
        if (getMenu() instanceof BlueprintTerminalMenu blueprintMenu) {
            return new BlueprintEncoderArea(blueprintMenu, font, leftPos, topPos, layout);
        }
        return layout.kind().hasCraftingGrid() ? new CraftingGridArea(getMenu().containerId, leftPos, topPos, layout)
                : null;
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
            final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        if (search != null) {
            search.draw(graphics, style, mouseX, mouseY);
        }
        if (grid != null) {
            final String query = search != null ? search.text() : "";
            final TerminalContents contents = getMenu().terminal().contents();
            grid.show(minecraft != null && Screen.hasShiftDown()
                    ? view.updateKeepingOrder(contents, settings, query)
                    : view.update(contents, settings, query));
            grid.draw(graphics, font, style, mouseX, mouseY);
        }
        drawStatus(graphics);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        if (workArea != null) {
            workArea.draw(graphics, style, mouseX, mouseY);
        }
        if (sidebar != null) {
            sidebar.draw(graphics, style, settings, mouseX, mouseY);
        }
    }

    private void drawStatus(final GuiGraphics graphics) {
        final TerminalStatus status = getMenu().terminal().contents().status();
        if (status == TerminalStatus.ONLINE) {
            return;
        }
        final Component message = Component.translatable(
                "gui.nexus.terminal." + status.name().toLowerCase(Locale.ROOT));
        final int centerX = leftPos + TerminalLayout.GRID_LEFT + layout.columns() * TerminalLayout.SLOT / 2;
        final int centerY = topPos + (TerminalLayout.GRID_TOP + layout.gridBottom() - font.lineHeight) / 2;
        graphics.drawCenteredString(font, message, centerX, centerY, PanelStyle.TEXT_LIGHT);
    }

    @Override
    public List<Rect2i> extraAreas() {
        return List.of(sidebar != null ? sidebar.area().toRect() : new Rect2i(leftPos, topPos, 0, 0));
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
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        final List<Component> lines = tooltipAt(mouseX, mouseY);
        if (!lines.isEmpty()) {
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private List<Component> tooltipAt(final int mouseX, final int mouseY) {
        final List<Component> area = workArea != null ? workArea.tooltip(mouseX, mouseY) : List.of();
        if (!area.isEmpty()) {
            return area;
        }
        final List<Component> searching = search != null ? search.tooltip(mouseX, mouseY) : List.of();
        if (!searching.isEmpty()) {
            return searching;
        }
        if (sidebar != null && sidebar.contains(mouseX, mouseY)) {
            return sidebar.tooltip(settings, mouseX, mouseY);
        }
        return grid != null && getMenu().getCarried().isEmpty()
                ? grid.tooltip(mouseX, mouseY, getMenu().terminal().contents()) : List.of();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        if (search != null && search.click(event.x(), event.y(), MouseButtons.isSecondary(event))) {
            return true;
        }
        if (search != null && getFocused() == search.widget() && !search.widget().isFocused()) {
            setFocused(null);
        }
        if (clickSidebar(event) || clickGrid(event)) {
            return true;
        }
        return workArea != null && workArea.click(event) || super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickSidebar(final MouseButtonEvent event) {
        final TerminalSettings changed = sidebar != null
                ? sidebar.click(settings, event.x(), event.y(), MouseButtons.isSecondary(event)) : null;
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
        final TerminalContents contents = getMenu().terminal().contents();
        final boolean asksToCraft = event.hasControlDown() || event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                || resource != null && contents.amountOf(resource) == 0;
        if (resource != null && contents.isCraftable(resource) && asksToCraft && getMenu().getCarried().isEmpty()) {
            openCraftRequest(resource);
            return true;
        }
        final GridClick click = event.hasShiftDown() ? GridClick.QUICK_MOVE
                : MouseButtons.isSecondary(event) ? GridClick.SECONDARY : GridClick.PRIMARY;
        if (click != GridClick.QUICK_MOVE || resource != null) {
            PacketDistributor.sendToServer(new TerminalClickPayload(getMenu().containerId, resource, click));
        }
        return true;
    }

    private void openCraftRequest(final NexusResource resource) {
        if (minecraft != null) {
            minecraft.setScreen(new CraftRequestScreen<>(this, getMenu(), resource));
        }
    }

    /**
     * Shows the new settings at once and lets the terminal keep them.
     */
    private void applySettings(final TerminalSettings changed) {
        final boolean resized = changed.size() != settings.size();
        settings = changed;
        PacketDistributor.sendToServer(new TerminalSettingsPayload(getMenu().containerId, changed));
        if (resized) {
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseDragged(
            final double mouseX, final double mouseY, final int button, final double dragX, final double dragY) {
        final MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        return grid != null && grid.drag(event.y()) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        final MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        if (grid != null) {
            grid.stopDrag();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        if (grid != null && grid.scroll(x, y, scrollY)) {
            return true;
        }
        return workArea != null && workArea.scroll(x, y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        final KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
        return search != null && search.keyPressed(event) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean hasClickedOutside(
            final double mouseX, final double mouseY, final int left, final int top, final int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button)
                && (sidebar == null || !sidebar.contains(mouseX, mouseY));
    }

    @Override
    public List<Rect2i> filterSlotAreas() {
        return workArea != null ? workArea.ghostAreas() : List.of();
    }

    @Override
    public @Nullable NexusResource filterEntryOf(final ItemStack stack) {
        return workArea != null ? workArea.ghostEntryOf(stack) : null;
    }

    @Override
    public @Nullable NexusResource filterEntryOf(final FluidStack fluid) {
        return workArea != null ? workArea.ghostEntryOf(fluid) : null;
    }

    @Override
    public void setFilterSlot(final int slot, final NexusResource resource) {
        if (workArea != null) {
            workArea.setGhost(slot, resource);
        }
    }

    /**
     * A resource of the grid under the cursor.
     *
     * @param area the icon's area on the screen
     */
    public record HoveredResource(NexusResource resource, Rect2i area) {
    }
}
