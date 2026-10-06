package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.menu.GeneratorMenu;
import com.morphengine.nexus.menu.GeneratorView;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Generator panel, tinted with the color of the network it feeds: the network, the input slot, a flame that burns
 * down or the tanks of the fluids it burns, the charge bar, what the generator is doing, and the upgrade slots on the
 * right.
 */
public final class GeneratorScreen extends PanelScreen<GeneratorMenu> {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = 192;
    private static final int FLAME_RGB = 0xFFD8843A;
    private static final int GAUGE_X = 41;
    private static final int FLAME_BAR_X = 51;
    private static final int FLAME_WIDTH = 6;
    private static final int TANK_WIDTH = 14;
    private static final int TANK_HEIGHT = 44;
    private static final int TANK_TOP = 40;
    private static final int TANK_GAP = 4;
    private static final int ICON_SIZE = 16;
    private static final int BAR_Y = GeneratorMenu.INPUT_SLOT_Y - 1;
    private static final int BAR_HEIGHT = ChargeBar.HEIGHT;
    /** The status may run on under the upgrade slots, but must end above the label of the inventory. */
    private static final int STATUS_RIGHT = 169;
    private static final int TEXT_GAP = 3;
    private static final int INVENTORY_LABEL_GAP = 11;
    private static final int TICKS_PER_SECOND = 20;
    /** The charge bar is as long in every generator and starts right after the gauges, which are few or many. */
    private static final int BAR_WIDTH = 90;

    private SideModePanel<MachineSide> sides;

    public GeneratorScreen(final GeneratorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.sides = createSideModePanel();
    }

    @Override
    protected void init() {
        super.init();
        sides = createSideModePanel();
    }

    private SideModePanel<MachineSide> createSideModePanel() {
        return new SideModePanel<>(panelBounds(), SideLayouts.MACHINE, SideLayouts.machineNames(),
                GeneratorMenu.BUTTON_SIDE_NEXT, GeneratorMenu.BUTTON_SIDE_PREVIOUS);
    }

    private PanelBounds flameBounds() {
        return new PanelBounds(leftPos + GAUGE_X, topPos + GeneratorMenu.INPUT_SLOT_Y - 1,
                FLAME_WIDTH, PanelStyle.SLOT_SIZE);
    }

    private PanelBounds tankBounds(final int index) {
        return new PanelBounds(leftPos + GAUGE_X + index * (TANK_WIDTH + TANK_GAP), topPos + TANK_TOP,
                TANK_WIDTH, TANK_HEIGHT);
    }

    /**
     * @return where the charge bar starts, after the flame or after the tanks
     */
    private int barX(final GeneratorView view) {
        final int gauges = view.tanks().size();
        return gauges == 0 ? FLAME_BAR_X : GAUGE_X + gauges * (TANK_WIDTH + TANK_GAP);
    }

    @Override
    protected PanelStyle style() {
        final NetworkBadge network = getMenu().view().network();
        return PanelStyle.of(network);
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final GeneratorView view = getMenu().view();
        PanelStyle.drawNetwork(graphics, font, view.network(), leftPos, topPos);
        sides.draw(graphics, font, style, getMenu().sideModes(), mouseX, mouseY);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        if (view.tanks().isEmpty()) {
            drawFlame(graphics, style, view);
        } else {
            for (int index = 0; index < view.tanks().size(); index++) {
                drawTank(graphics, style, tankBounds(index), view.tanks().get(index));
            }
        }
        drawChargeBar(graphics, style, view);
        drawStatus(graphics, view);
        graphics.text(font, playerInventoryTitle, leftPos + GeneratorMenu.INVENTORY_LEFT,
                topPos + GeneratorMenu.INVENTORY_TOP - INVENTORY_LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    private void drawFlame(final GuiGraphicsExtractor graphics, final PanelStyle style, final GeneratorView view) {
        final PanelBounds flame = flameBounds();
        final int bottom = flame.top() + flame.height();
        graphics.fill(flame.left(), flame.top(), flame.left() + flame.width(), bottom, style.track());
        if (view.burnTicksTotal() > 0) {
            final int height = flame.height() * view.burnTicksLeft() / view.burnTicksTotal();
            graphics.fill(flame.left(), bottom - height, flame.left() + flame.width(), bottom, FLAME_RGB);
        }
        graphics.outline(flame.left(), flame.top(), flame.width(), flame.height(), style.border());
    }

    /**
     * A tank as a bar that fills from below with the still texture of its fluid.
     */
    private void drawTank(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final PanelBounds bounds,
            final TankView tank) {
        graphics.fill(bounds.left(), bounds.top(), bounds.left() + bounds.width(), bounds.top() + bounds.height(),
                style.track());
        final int filled = tank.capacity() > 0 && tank.fluid() != null
                ? (int) ((bounds.height() - 2) * tank.amount() / tank.capacity()) : 0;
        if (filled > 0) {
            final int bottom = bounds.top() + bounds.height() - 1;
            graphics.enableScissor(bounds.left() + 1, bottom - filled, bounds.left() + bounds.width() - 1, bottom);
            final ResourceIcon icon = ResourceRenderers.icon(tank.fluid());
            for (int y = bottom - ICON_SIZE; y > bottom - filled - ICON_SIZE; y -= ICON_SIZE) {
                icon.draw(graphics, bounds.left() + (bounds.width() - ICON_SIZE) / 2, y);
            }
            graphics.disableScissor();
        }
        graphics.outline(bounds.left(), bounds.top(), bounds.width(), bounds.height(), style.border());
    }

    private void drawChargeBar(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final GeneratorView view) {
        final int x = leftPos + barX(view);
        final int y = topPos + BAR_Y;
        final ChargeBar bar = ChargeBar.at(x, y, BAR_WIDTH);
        bar.draw(graphics, style, view.stored(), view.capacity());
        bar.label(graphics, font,
                EnergyFormat.amount(view.stored()) + " / " + EnergyFormat.amount(view.capacity()) + " FE");
    }

    private void drawStatus(final GuiGraphicsExtractor graphics, final GeneratorView view) {
        final Component status = switch (view.status()) {
            case GENERATING -> Component.translatable("gui.nexus.generator.generating",
                    EnergyFormat.amount(view.production()));
            case BUFFER_FULL -> Component.translatable("gui.nexus.generator.full");
            case NO_FUEL -> Component.translatable("gui.nexus.generator.no_fuel");
        };
        graphics.textWithWordWrap(font, status, leftPos + barX(view),
                topPos + BAR_Y + BAR_HEIGHT + 2 * TEXT_GAP + font.lineHeight, STATUS_RIGHT - barX(view),
                PanelStyle.TEXT_DIM);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        sides.showTooltip(graphics, font, getMenu().sideModes(), mouseX, mouseY);
        final GeneratorView view = getMenu().view();
        if (view.tanks().isEmpty()) {
            if (flameBounds().contains(mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(font, burnTooltip(view), mouseX, mouseY);
            }
            return;
        }
        for (int index = 0; index < view.tanks().size(); index++) {
            if (tankBounds(index).contains(mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(font,
                        tankTooltip(view.tanks().get(index), view.accepted().get(index)), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return minecraft != null
                && sides.click(minecraft, getMenu().containerId, event.x(), event.y(), event.button())
                || super.mouseClicked(event, doubleClick);
    }

    private static List<Component> tankTooltip(final TankView tank, final FluidKey accepted) {
        if (tank.fluid() == null) {
            return List.of(Component.translatable("gui.nexus.generator.tank_accepts", accepted.name()));
        }
        return List.of(tank.fluid().name(), Component.translatable("gui.nexus.generator.tank_amount",
                EnergyFormat.amount(tank.amount()), EnergyFormat.amount(tank.capacity())));
    }

    private static Component burnTooltip(final GeneratorView view) {
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
