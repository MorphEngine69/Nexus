package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.CoalGeneratorMenu;
import com.morphengine.nexus.menu.CoalGeneratorView;
import com.morphengine.nexus.menu.NetworkBadge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Coal Generator panel, tinted with the color of the network it feeds: the
 * network, the fuel slot with a flame that burns down, the charge bar, what
 * the generator is doing, and the upgrade slots on the right.
 */
public final class CoalGeneratorScreen extends PanelScreen<CoalGeneratorMenu> {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = 192;
    private static final int FLAME_RGB = 0xFFD8843A;
    private static final int FLAME_X = 52;
    private static final int FLAME_WIDTH = 6;
    private static final int BAR_X = 70;
    private static final int BAR_Y = 38;
    private static final int BAR_HEIGHT = 12;
    private static final int LINE_HEIGHT = 11;
    private static final int TEXT_GAP = 3;
    private static final int INVENTORY_LABEL_GAP = 11;
    private static final int TICKS_PER_SECOND = 20;
    private static final int BAR_GAP = 6;

    public CoalGeneratorScreen(final CoalGeneratorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private PanelBounds flameBounds() {
        return new PanelBounds(leftPos + FLAME_X, topPos + CoalGeneratorMenu.FUEL_SLOT_Y - 1,
                FLAME_WIDTH, PanelStyle.SLOT_SIZE);
    }

    @Override
    protected PanelStyle style() {
        final NetworkBadge network = getMenu().view().network();
        return PanelStyle.of(network);
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final CoalGeneratorView view = getMenu().view();
        final NetworkBadge network = view.network();
        PanelStyle.drawNetwork(graphics, font, network, leftPos, topPos);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        drawFlame(graphics, style, view);
        drawChargeBar(graphics, style, view);
        drawStatus(graphics, view);
        graphics.text(font, playerInventoryTitle, leftPos + CoalGeneratorMenu.INVENTORY_LEFT,
                topPos + CoalGeneratorMenu.INVENTORY_TOP - INVENTORY_LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    private void drawFlame(final GuiGraphicsExtractor graphics, final PanelStyle style, final CoalGeneratorView view) {
        final PanelBounds flame = flameBounds();
        final int bottom = flame.top() + flame.height();
        graphics.fill(flame.left(), flame.top(), flame.left() + flame.width(), bottom, style.track());
        if (view.burnTicksTotal() > 0) {
            final int height = flame.height() * view.burnTicksLeft() / view.burnTicksTotal();
            graphics.fill(flame.left(), bottom - height, flame.left() + flame.width(), bottom, FLAME_RGB);
        }
        graphics.outline(flame.left(), flame.top(), flame.width(), flame.height(), style.border());
    }

    private void drawChargeBar(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final CoalGeneratorView view) {
        final int x = leftPos + BAR_X;
        final int y = topPos + BAR_Y;
        final int width = CoalGeneratorMenu.UPGRADE_SLOT_X - 1 - BAR_GAP - BAR_X;
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, style.track());
        final long filled = view.capacity() > 0 ? (width - 2) * view.stored() / view.capacity() : 0;
        if (filled > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + (int) filled, y + BAR_HEIGHT - 1, style.accent());
        }
        graphics.outline(x, y, width, BAR_HEIGHT, style.border());
        graphics.text(font, EnergyFormat.amount(view.stored()) + " / " + EnergyFormat.amount(view.capacity()) + " FE",
                x, y + BAR_HEIGHT + TEXT_GAP, PanelStyle.TEXT_LIGHT, false);
    }

    private void drawStatus(final GuiGraphicsExtractor graphics, final CoalGeneratorView view) {
        final Component status = switch (view.status()) {
            case GENERATING -> Component.translatable("gui.nexus.generator.generating",
                    EnergyFormat.amount(view.production()));
            case BUFFER_FULL -> Component.translatable("gui.nexus.generator.full");
            case NO_FUEL -> Component.translatable("gui.nexus.generator.no_fuel");
        };
        graphics.textWithWordWrap(font, status, leftPos + BAR_X,
                topPos + BAR_Y + BAR_HEIGHT + TEXT_GAP + LINE_HEIGHT,
                CoalGeneratorMenu.UPGRADE_SLOT_X - BAR_GAP - BAR_X, PanelStyle.TEXT_DIM);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (flameBounds().contains(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, burnTooltip(getMenu().view()), mouseX, mouseY);
        }
    }

    private static Component burnTooltip(final CoalGeneratorView view) {
        if (view.burnTicksLeft() == 0) {
            return Component.translatable("gui.nexus.generator.not_burning");
        }
        return Component.translatable("gui.nexus.generator.burn_left",
                seconds(view.burnTicksLeft()), seconds(view.burnTicksTotal()));
    }

    private static int seconds(final int ticks) {
        return (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }
}
