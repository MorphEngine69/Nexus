package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.menu.EnergyCellView;
import com.morphengine.nexus.menu.NetworkBadge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Charge of one Energy Cell, tinted with the color of the network it belongs
 * to, and its priority in the network's energy pool.
 */
public final class EnergyCellScreen extends PanelScreen<EnergyCellMenu> {

    private static final int IMAGE_WIDTH = 236;
    private static final int IMAGE_HEIGHT = EnergyCellMenu.INVENTORY_TOP + 84;
    private static final int LABEL_GAP = 11;
    private static final int BAR_HEIGHT = 16;
    private static final int GAP = 8;
    private static final int PERCENT = 100;
    private static final int PRIORITY_TOP = 32;
    private static final int BAR_TOP = 52;

    private final StatLine statLine = new StatLine();
    private PriorityRow priority;
    private SideModePanel<Direction> sides;

    public EnergyCellScreen(final EnergyCellMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.priority = createPriorityRow();
        this.sides = createSideModePanel();
    }

    @Override
    protected void init() {
        super.init();
        priority = createPriorityRow();
        sides = createSideModePanel();
    }

    private SideModePanel<Direction> createSideModePanel() {
        return new SideModePanel<>(panelBounds(), SideLayouts.CUBE, SideLayouts.cubeNames(),
                EnergyCellMenu.BUTTON_SIDE_NEXT, EnergyCellMenu.BUTTON_SIDE_PREVIOUS);
    }

    private PriorityRow createPriorityRow() {
        return new PriorityRow(panelBounds(), topPos + PRIORITY_TOP, EnergyCellMenu.BUTTON_PRIORITY);
    }

    @Override
    protected PanelStyle style() {
        final NetworkBadge network = getMenu().view().network();
        return PanelStyle.of(network);
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final EnergyCellView view = getMenu().view();
        final NetworkBadge network = view.network();
        final int x = leftPos + PanelStyle.PADDING;
        PanelStyle.drawNetwork(graphics, font, network, leftPos, topPos);
        priority.draw(graphics, font, style, getMenu().priority());
        sides.draw(graphics, font, style, getMenu().sideModes(), mouseX, mouseY);

        int y = topPos + BAR_TOP;
        drawChargeBar(graphics, style, view,
                new PanelBounds(x, y, EnergyCellMenu.UPGRADES_LEFT - PanelStyle.PADDING * 2, BAR_HEIGHT));

        y += BAR_HEIGHT + GAP;
        final int textWidth = EnergyCellMenu.UPGRADES_LEFT - PanelStyle.PADDING * 2;
        statLine.begin();
        y += statLine.draw(graphics, font, EnergyFormat.stored(view.stored(), view.capacity()),
                new PanelBounds(x, y, textWidth, 0), PanelStyle.TEXT_LIGHT);
        y += statLine.draw(graphics, font, EnergyFormat.rate("input", view.input()),
                new PanelBounds(x, y, textWidth, 0), PanelStyle.TEXT_DIM);
        statLine.draw(graphics, font, EnergyFormat.rate("output", view.output()),
                new PanelBounds(x, y, textWidth, 0), PanelStyle.TEXT_DIM);

        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        graphics.text(font, playerInventoryTitle, leftPos + EnergyCellMenu.INVENTORY_LEFT,
                topPos + EnergyCellMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        statLine.showTooltip(graphics, font, mouseX, mouseY);
        sides.showTooltip(graphics, font, getMenu().sideModes(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return minecraft != null && (priority.click(minecraft, getMenu().containerId, event.x(), event.y())
                || sides.click(minecraft, getMenu().containerId, event.x(), event.y(), event.button()))
                || super.mouseClicked(event, doubleClick);
    }

    private void drawChargeBar(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final EnergyCellView view,
            final PanelBounds bar) {
        graphics.fill(bar.left(), bar.top(), bar.left() + bar.width(), bar.top() + bar.height(), style.track());
        final int inner = bar.width() - 2;
        final int filled = view.capacity() > 0 ? (int) (inner * view.stored() / view.capacity()) : 0;
        if (filled > 0) {
            graphics.fill(bar.left() + 1, bar.top() + 1, bar.left() + 1 + filled, bar.top() + bar.height() - 1,
                    style.accent());
        }
        graphics.outline(bar.left(), bar.top(), bar.width(), bar.height(), style.border());
        final long percent = view.capacity() > 0 ? view.stored() * PERCENT / view.capacity() : 0;
        final String label = percent + "%";
        graphics.text(font, label, bar.left() + (bar.width() - font.width(label)) / 2,
                bar.top() + (bar.height() - font.lineHeight) / 2 + 1, PanelStyle.TEXT_LIGHT, true);
    }
}
