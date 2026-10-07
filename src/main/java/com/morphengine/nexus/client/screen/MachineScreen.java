package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.machine.MachineActivity;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.menu.MachineMenu;
import com.morphengine.nexus.menu.MachineView;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.processing.ItemStackSlots;
import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Panel of a machine, tinted with the color of its network: the charge of its buffer, a column for every line with
 * a bar of its progress between the input slot and the output slot, what the machine is doing and how fast, the slot
 * of the Speed Upgrades, the button of the input mode on the left and the choice of the working sides on the right.
 */
public final class MachineScreen extends PanelScreen<MachineMenu> implements SideAreas {

    private static final int BAR_TOP = 32;
    private static final int LABEL_GAP = 11;
    private static final int PERCENT = 100;
    private static final int ARROW_LENGTH = 22;
    private static final int ARROW_HEAD_ROWS = 5;
    private static final int ARROW_SHAFT_HALF = 2;
    private static final float SMOOTHING = 0.25F;
    private static final int STATUS_GAP = 3;
    private static final int TANK_WIDTH = 120;
    private static final int TANK_HEIGHT = 10;
    /** A tank with anything in it shows at least this much, however big the tank is. */
    private static final int MIN_FILL = 3;
    private static final int FLUID_TILE = 16;

    private final float[] shown = new float[ItemStackSlots.MAX_LINES];
    private InputModeButton modeButton;
    private SideModePanel<MachineSide> sides;

    public MachineScreen(final MachineMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, MachineMenu.IMAGE_WIDTH, MachineMenu.IMAGE_HEIGHT);
        this.modeButton = createModeButton();
        this.sides = createSideModePanel();
    }

    @Override
    protected void init() {
        super.init();
        modeButton = createModeButton();
        sides = createSideModePanel();
    }

    private SideButtons redstoneButton() {
        return new SideButtons(leftPos, topPos, 1);
    }

    private Identifier redstoneIcon() {
        return Identifier.fromNamespaceAndPath(Nexus.MOD_ID,
                "transfer/redstone_" + getMenu().redstoneMode().name().toLowerCase(Locale.ROOT));
    }

    /** A machine with one line has nothing to share between lines, so it has no input mode to choose. */
    private boolean hasModeButton() {
        return getMenu().hasInputMode();
    }

    private InputModeButton createModeButton() {
        return new InputModeButton(panelBounds(), MachineMenu.BUTTON_MODE);
    }

    private SideModePanel<MachineSide> createSideModePanel() {
        return new SideModePanel<>(panelBounds(), SideLayouts.MACHINE, SideLayouts.machineNames(),
                MachineMenu.BUTTON_SIDE_NEXT, MachineMenu.BUTTON_SIDE_PREVIOUS);
    }

    @Override
    public List<Rect2i> extraAreas() {
        final List<Rect2i> areas = new ArrayList<>();
        areas.add(redstoneButton().area().toRect());
        if (hasModeButton()) {
            areas.add(modeButton.area());
        }
        areas.addAll(sides.areas());
        return areas;
    }

    @Override
    protected PanelStyle style() {
        final NetworkBadge network = getMenu().view().network();
        return PanelStyle.of(network);
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final MachineView view = getMenu().view();
        PanelStyle.drawNetworkOrStandalone(graphics, font, view.network(), leftPos, topPos);
        redstoneButton().draw(graphics, style, 0, redstoneIcon(), redstoneButton().buttonAt(mouseX, mouseY) == 0);
        if (hasModeButton()) {
            modeButton.draw(graphics, style, getMenu().inputMode(), mouseX, mouseY);
        }
        sides.draw(graphics, font, style, getMenu().sideModes(), mouseX, mouseY);
        drawChargeBar(graphics, style, view);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        for (int line = 0; line < getMenu().pairs(); line++) {
            drawProgress(graphics, style, line, view.progressOf(line));
        }
        drawStatus(graphics, view);
        if (view.tank() != null) {
            drawTank(graphics, style, view.tank());
        }
        graphics.text(font, playerInventoryTitle, leftPos + MachineMenu.INVENTORY_LEFT,
                topPos + MachineMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    private void drawChargeBar(final GuiGraphicsExtractor graphics, final PanelStyle style, final MachineView view) {
        final int x = leftPos + PanelStyle.PADDING;
        final int y = topPos + BAR_TOP;
        final int width = MachineMenu.IMAGE_WIDTH - 2 * PanelStyle.PADDING;
        final ChargeBar bar = ChargeBar.at(x, y, width);
        bar.draw(graphics, style, view.stored(), view.capacity());
        final String label = EnergyFormat.amount(view.stored()) + " / " + EnergyFormat.amount(view.capacity()) + " FE";
        bar.label(graphics, font, label);
    }

    /**
     * An arrow pointing down between the input slot and the output slot of a line, which fills from the top. The shown
     * value follows the one the server sent, which comes once a tick, so that the arrow fills smoothly.
     */
    private void drawProgress(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int line, final int percent) {
        shown[line] = percent < shown[line] ? percent : shown[line] + (percent - shown[line]) * SMOOTHING;
        final int middle = leftPos + MachineMenu.outputX(line, getMenu().inputsPerLine(), getMenu().inputCount())
                + PanelStyle.SLOT_SIZE / 2;
        final int top = topPos + MachineMenu.LINES_TOP + PanelStyle.SLOT_SIZE
                + (MachineMenu.OUTPUT_OFFSET - PanelStyle.SLOT_SIZE - ARROW_LENGTH) / 2;
        final int filled = Math.round(ARROW_LENGTH * shown[line] / PERCENT);
        for (int row = 0; row < ARROW_LENGTH; row++) {
            final int fromEnd = ARROW_LENGTH - row;
            final int half = fromEnd <= ARROW_HEAD_ROWS ? fromEnd : ARROW_SHAFT_HALF;
            graphics.fill(middle - half, top + row, middle + half, top + row + 1,
                    row < filled ? style.accent() : style.track());
        }
    }

    /**
     * The tank of a machine that gives a fluid: a bar under the arrow, with the fluid and how much of it there is
     * written below.
     */
    private void drawTank(final GuiGraphicsExtractor graphics, final PanelStyle style, final TankView tank) {
        final int x = leftPos + MachineMenu.outputX(0, 1, 1) - (TANK_WIDTH - PanelStyle.SLOT_SIZE) / 2;
        final int y = topPos + MachineMenu.LINES_TOP + MachineMenu.OUTPUT_OFFSET;
        graphics.fill(x, y, x + TANK_WIDTH, y + TANK_HEIGHT, style.track());
        final int filled = tank.amount() > 0
                ? Math.max(MIN_FILL, (int) ((TANK_WIDTH - 2) * tank.amount() / tank.capacity())) : 0;
        if (filled > 0 && tank.fluid() != null) {
            drawFluid(graphics, tank.fluid(), x + 1, y + 1, filled, TANK_HEIGHT - 2);
        }
        graphics.outline(x, y, TANK_WIDTH, TANK_HEIGHT, style.border());
        final Component fluid = tank.fluid() != null ? tank.fluid().name()
                : Component.translatable("gui.nexus.machine.tank_empty");
        final Component text = Component.translatable("gui.nexus.machine.tank", fluid,
                EnergyFormat.amount(tank.amount()), EnergyFormat.amount(tank.capacity()));
        graphics.text(font, text, x + (TANK_WIDTH - font.width(text)) / 2, y + TANK_HEIGHT + STATUS_GAP,
                PanelStyle.TEXT_LIGHT, false);
    }

    /**
     * The still texture of the fluid, tiled over the filled part of the bar, as the tanks of the generators show it.
     */
    private static void drawFluid(
            final GuiGraphicsExtractor graphics, final FluidKey fluid, final int left, final int top, final int width,
            final int height) {
        final ResourceIcon icon = ResourceRenderers.icon(fluid);
        graphics.enableScissor(left, top, left + width, top + height);
        for (int tileX = left; tileX < left + width; tileX += FLUID_TILE) {
            icon.draw(graphics, tileX, top + (height - FLUID_TILE) / 2);
        }
        graphics.disableScissor();
    }

    private void drawStatus(final GuiGraphicsExtractor graphics, final MachineView view) {
        final int y = topPos + BAR_TOP + ChargeBar.HEIGHT + STATUS_GAP;
        final Component status = Component.translatable("gui.nexus.machine.status."
                + view.activity().name().toLowerCase(Locale.ROOT));
        graphics.text(font, status, leftPos + PanelStyle.PADDING, y + font.lineHeight + STATUS_GAP,
                view.activity() == MachineActivity.WORKING ? PanelStyle.TEXT_LIGHT : PanelStyle.TEXT_DIM, false);
        final Component speed = Component.translatable("gui.nexus.machine.speed", view.speedPercent());
        graphics.text(font, speed, leftPos + MachineMenu.IMAGE_WIDTH - PanelStyle.PADDING - font.width(speed), y,
                PanelStyle.TEXT_DIM, false);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hasModeButton()) {
            modeButton.showTooltip(graphics, font, getMenu().inputMode(), mouseX, mouseY);
        }
        sides.showTooltip(graphics, font, getMenu().sideModes(), mouseX, mouseY);
        if (redstoneButton().buttonAt(mouseX, mouseY) == 0) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.nexus.transfer.redstone"),
                    Component.translatable("gui.nexus.transfer.redstone."
                            + getMenu().redstoneMode().name().toLowerCase(Locale.ROOT))
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return clickedOwnControl(event) || super.mouseClicked(event, doubleClick);
    }

    private boolean clickedOwnControl(final MouseButtonEvent event) {
        if (minecraft == null) {
            return false;
        }
        final int container = getMenu().containerId;
        if (redstoneButton().buttonAt(event.x(), event.y()) == 0 && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(container, MachineMenu.BUTTON_REDSTONE);
            return true;
        }
        return hasModeButton() && modeButton.click(minecraft, container, event.x(), event.y())
                || sides.click(minecraft, container, event.x(), event.y(), event.button());
    }
}
