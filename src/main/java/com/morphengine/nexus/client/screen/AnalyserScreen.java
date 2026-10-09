package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.analysis.AnalyserLine;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.menu.AnalyserMenu;
import com.morphengine.nexus.menu.AnalyserView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * The panel of an Analyser, tinted with the color of the network of the block it looks at: the block's name in the
 * header, the network it works in, and what the block tells about itself, group by group, as lines of a name and its
 * value. A long report scrolls with the wheel. A block with a buffer shows how full it is under the lines.
 */
public final class AnalyserScreen extends PanelScreen<AnalyserMenu> {

    private static final int IMAGE_WIDTH = 230;
    private static final int IMAGE_HEIGHT = 240;
    private static final int FIRST_LINE_TOP = 34;
    private static final int LINE_HEIGHT = 11;
    private static final int VISIBLE_LINES = 16;
    private static final int BAR_TOP = 218;
    private static final int LABEL_SHARE_PERCENT = 55;
    private static final int PERCENT = 100;
    private static final int VALUE_GAP = 6;

    private int scroll;

    public AnalyserScreen(final AnalyserMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected Component panelTitle() {
        final Component name = getMenu().view().name();
        return name.getString().isEmpty() ? super.panelTitle() : name;
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().view().network());
    }

    @Override
    protected void extractPanel(
            final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final AnalyserView view = getMenu().view();
        if (view.kind() == AnalyserView.Kind.NOTHING) {
            graphics.drawString(font, Component.translatable("gui.nexus.access.loading"), leftPos + PanelStyle.PADDING,
                    topPos + FIRST_LINE_TOP, PanelStyle.TEXT_DIM, false);
            return;
        }
        if (view.kind() == AnalyserView.Kind.NEXUS) {
            PanelStyle.drawNetwork(graphics, font, view.network(), leftPos, topPos);
        } else {
            PanelStyle.drawNetworkOrStandalone(graphics, font, view.network(), leftPos, topPos);
        }
        drawLines(graphics, style, view.lines());
        final NetworkStatistics energy = view.energy();
        if (view.kind() == AnalyserView.Kind.NEXUS || energy.energyCapacity() > 0) {
            final ChargeBar bar = ChargeBar.at(leftPos + PanelStyle.PADDING, topPos + BAR_TOP,
                    imageWidth - PanelStyle.PADDING * 2);
            bar.draw(graphics, style, energy.energyStored(), energy.energyCapacity());
            bar.label(graphics, font, EnergyFormat.amount(energy.energyStored()) + " / "
                    + EnergyFormat.amount(energy.energyCapacity()) + " FE");
        }
    }

    private void drawLines(
            final GuiGraphics graphics, final PanelStyle style, final List<AnalyserLine> lines) {
        scroll = Math.max(0, Math.min(scroll, lines.size() - VISIBLE_LINES));
        final int left = leftPos + PanelStyle.PADDING;
        final int width = imageWidth - PanelStyle.PADDING * 2;
        for (int row = 0; row < Math.min(VISIBLE_LINES, lines.size() - scroll); row++) {
            final AnalyserLine line = lines.get(scroll + row);
            final int y = topPos + FIRST_LINE_TOP + row * LINE_HEIGHT;
            if (line.isHeading()) {
                graphics.drawString(font, line.label(), left, y, style.accent(), false);
                continue;
            }
            final int labelWidth = width * LABEL_SHARE_PERCENT / PERCENT;
            graphics.drawString(font, font.plainSubstrByWidth(line.label().getString(), labelWidth), left, y,
                    PanelStyle.TEXT_DIM, false);
            final String value = font.plainSubstrByWidth(line.value().getString(), width - labelWidth + VALUE_GAP);
            graphics.drawString(font, value, left + width - font.width(value), y, PanelStyle.TEXT_LIGHT, false);
        }
        if (lines.size() > VISIBLE_LINES) {
            final String position = (scroll + 1) + "-" + Math.min(lines.size(), scroll + VISIBLE_LINES) + " / "
                    + lines.size();
            graphics.drawString(font, position, left + width - font.width(position),
                    topPos + FIRST_LINE_TOP - LINE_HEIGHT - 1, PanelStyle.TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        final int maximum = Math.max(0, getMenu().view().lines().size() - VISIBLE_LINES);
        scroll = Math.max(0, Math.min(maximum, scroll - (int) Math.signum(scrollY)));
        return true;
    }
}
