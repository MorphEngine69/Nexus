package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.menu.EnergyCellView;
import com.morphengine.nexus.menu.NetworkBadge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Charge of one Energy Cell, tinted with the color of the network it belongs to.
 */
public final class EnergyCellScreen extends PanelScreen<EnergyCellMenu> {

    private static final int IMAGE_WIDTH = 236;
    private static final int IMAGE_HEIGHT = 110;
    private static final int LINE_HEIGHT = 12;
    private static final int BAR_HEIGHT = 16;
    private static final int GAP = 8;
    private static final int PERCENT = 100;

    public EnergyCellScreen(final EnergyCellMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
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
        int y = topPos + PanelStyle.NETWORK_TOP;
        PanelStyle.drawNetwork(graphics, font, network, leftPos, topPos);

        y += LINE_HEIGHT + GAP;
        drawChargeBar(graphics, style, view, new PanelBounds(x, y, imageWidth - PanelStyle.PADDING * 2, BAR_HEIGHT));

        y += BAR_HEIGHT + GAP;
        graphics.text(font, Component.translatable("gui.nexus.stats.energy",
                EnergyFormat.amount(view.stored()) + " / " + EnergyFormat.amount(view.capacity())),
                x, y, PanelStyle.TEXT_LIGHT, false);
        y += LINE_HEIGHT;
        graphics.text(font, Component.translatable("gui.nexus.stats.input", EnergyFormat.amount(view.input())),
                x, y, PanelStyle.TEXT_DIM, false);
        y += LINE_HEIGHT;
        graphics.text(font, Component.translatable("gui.nexus.stats.output", EnergyFormat.amount(view.output())),
                x, y, PanelStyle.TEXT_DIM, false);
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
