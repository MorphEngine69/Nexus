package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.TerminalPanel;
import com.morphengine.nexus.networking.CraftRequestPayload;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.terminal.CraftRequest;
import com.morphengine.nexus.terminal.PlanPreview;
import com.morphengine.nexus.terminal.TerminalContents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Asks the network to craft a resource, over the terminal it was opened from:
 * the amount, typed or stepped with the buttons, and the plan for it, worked
 * out by the server a moment after the amount last changed. Each resource of
 * the plan shows what is taken from storage, what is crafted and what is
 * missing. Start begins the task when nothing is missing and goes back to the
 * terminal; so do Escape and Cancel, without starting anything. When something
 * is missing, Craft Less brings the amount down to the most that can start.
 *
 * @param <M> the terminal's menu, which stays open underneath
 */
final class CraftRequestScreen<M extends AbstractContainerMenu & TerminalPanel> extends Screen {

    private static final int WIDTH = 240;
    private static final int HEIGHT = 222;
    private static final int AMOUNT_TOP = 22;
    private static final int PLAN_TOP = 62;
    private static final int ROW_HEIGHT = 18;
    private static final int ROWS = 7;
    private static final int[] STEPS = {1, 10, 64, -1, -10, -64};
    private static final int PREVIEW_DELAY_TICKS = 5;
    private static final int MISSING_RGB = 0xFFE05A4E;
    private static final int CRAFTED_RGB = 0xFF7FC7FF;
    private static final int MAX_DIGITS = 9;
    private static final int AMOUNT_LEFT = PanelStyle.PADDING + 22;
    private static final int AMOUNT_WIDTH = 80;
    private static final int TEXT_INSET = 4;
    private static final int TEXT_GAP = 6;

    private final Screen parent;
    private final M menu;
    private final NexusResource resource;
    private @Nullable EditBox amountBox;
    private long amount = 1;
    private int ticksSinceChange;
    private boolean previewSent;
    private int seenPlanRevision;
    private int scroll;

    CraftRequestScreen(final Screen parent, final M menu, final NexusResource resource) {
        super(Component.translatable("gui.nexus.craft.title", resource.name()));
        this.parent = parent;
        this.menu = menu;
        this.resource = resource;
        this.seenPlanRevision = contents().planRevision();
    }

    private TerminalContents contents() {
        return menu.terminal().contents();
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return (height - HEIGHT) / 2;
    }

    private CraftRequestButtons buttons() {
        return new CraftRequestButtons(new PanelBounds(left(), top(), WIDTH, HEIGHT));
    }

    @Override
    protected void init() {
        final EditBox box = new EditBox(font, left() + AMOUNT_LEFT, top() + AMOUNT_TOP, AMOUNT_WIDTH,
                CraftRequestButtons.HEIGHT, Component.translatable("gui.nexus.craft.amount"));
        box.setMaxLength(MAX_DIGITS);
        box.setValue(Long.toString(amount));
        box.setResponder(this::amountTyped);
        amountBox = addRenderableWidget(box);
        setInitialFocus(box);
    }

    private void amountTyped(final String typed) {
        final String digits = typed.replaceAll("[^0-9]", "");
        final long parsed = digits.isEmpty() ? 0 : Long.parseLong(digits);
        if (parsed != amount) {
            amount = parsed;
            amountChanged();
        }
    }

    private void amountChanged() {
        ticksSinceChange = 0;
        previewSent = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!previewSent && ++ticksSinceChange >= PREVIEW_DELAY_TICKS && amount > 0) {
            send(CraftRequest.PREVIEW);
            previewSent = true;
        }
        if (contents().planRevision() != seenPlanRevision) {
            seenPlanRevision = contents().planRevision();
            if (contents().planOutcome() == CraftRequest.START && minecraft != null) {
                minecraft.gui.setScreen(parent);
            } else if (contents().planOutcome() == CraftRequest.CRAFT_LESS) {
                takeAmountOf(contents().plan());
            }
        }
    }

    /**
     * Shows the amount of the plan received for Craft Less as the amount asked,
     * without planning it again.
     */
    private void takeAmountOf(final @Nullable PlanPreview plan) {
        if (plan == null || !plan.target().resource().equals(resource)) {
            return;
        }
        amount = plan.target().amount();
        if (amountBox != null) {
            amountBox.setValue(Long.toString(amount));
        }
    }

    private void send(final CraftRequest request) {
        ClientPacketDistributor.sendToServer(new CraftRequestPayload(menu.containerId, resource, amount, request));
    }

    /**
     * @return the plan received for the resource and amount shown now; {@code null} until it arrives
     */
    private @Nullable PlanPreview currentPlan() {
        final PlanPreview plan = contents().plan();
        final boolean current = plan != null && previewSent && plan.target().resource().equals(resource)
                && plan.target().amount() == amount;
        return current ? plan : null;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                                   final float partialTick) {
        final PanelStyle style = PanelStyle.of(menu.badge());
        final CraftRequestButtons buttons = buttons();
        style.drawFrame(graphics, font, buttons.panel(), title);
        ResourceRenderers.icon(resource).draw(graphics, left() + PanelStyle.PADDING, top() + AMOUNT_TOP - 1);
        for (int i = 0; i < STEPS.length; i++) {
            style.drawButton(graphics, font, buttons.step(i, STEPS.length),
                    Component.literal((STEPS[i] > 0 ? "+" : "") + STEPS[i]));
        }
        drawPlan(graphics, style);
        style.drawButton(graphics, font, buttons.cancel(), Component.translatable("gui.nexus.craft.cancel"));
        final PlanPreview plan = currentPlan();
        if (canCraftLess(plan)) {
            style.drawButton(graphics, font, buttons.craftLess(), Component.translatable("gui.nexus.craft.craft_less"));
        }
        style.drawButton(graphics, font, buttons.start(), Component.translatable(plan != null && plan.isComplete()
                ? "gui.nexus.craft.start" : "gui.nexus.craft.cannot_start"));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPlan(final GuiGraphicsExtractor graphics, final PanelStyle style) {
        final int rowsLeft = left() + PanelStyle.PADDING;
        final PlanPreview plan = currentPlan();
        if (plan == null) {
            final Component waiting = Component.translatable(amount > 0 ? "gui.nexus.craft.planning"
                    : "gui.nexus.craft.no_amount");
            graphics.text(font, waiting, rowsLeft, top() + PLAN_TOP + TEXT_INSET, PanelStyle.TEXT_DIM, false);
            return;
        }
        final List<PlanRow> rows = PlanRow.rowsOf(plan);
        scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - ROWS));
        for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
            final int y = top() + PLAN_TOP + i * ROW_HEIGHT;
            style.drawSlot(graphics, rowsLeft, y);
            drawRow(graphics, rows.get(scroll + i), rowsLeft, y);
        }
    }

    private void drawRow(final GuiGraphicsExtractor graphics, final PlanRow row, final int x, final int y) {
        final NexusResource shown = NexusResources.of(row.resource());
        ResourceRenderers.icon(shown).draw(graphics, x + 1, y + 1);
        final int textLeft = x + PanelStyle.SLOT_SIZE + TEXT_INSET;
        final int textTop = y + (PanelStyle.SLOT_SIZE - font.lineHeight) / 2 + 1;
        int column = textLeft;
        column = drawAmount(graphics, "gui.nexus.craft.from_storage", row.fromStorage(), shown, column, textTop,
                PanelStyle.TEXT_LIGHT);
        column = drawAmount(graphics, "gui.nexus.craft.crafted", row.crafted(), shown, column, textTop, CRAFTED_RGB);
        drawAmount(graphics, "gui.nexus.craft.missing", row.missing(), shown, column, textTop, MISSING_RGB);
    }

    private int drawAmount(final GuiGraphicsExtractor graphics, final String key, final long value,
                           final NexusResource shown, final int x, final int y, final int color) {
        if (value <= 0) {
            return x;
        }
        final Component text = Component.translatable(key, shown.type().unit().compact(value));
        graphics.text(font, text, x, y, color, false);
        return x + font.width(text) + TEXT_GAP;
    }

    /**
     * @return whether a smaller amount might start where {@code plan} cannot
     */
    private boolean canCraftLess(final @Nullable PlanPreview plan) {
        return plan != null && !plan.missing().isEmpty() && amount > 1;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final CraftRequestButtons buttons = buttons();
        return clickStep(buttons, event.x(), event.y()) || clickAction(buttons, event.x(), event.y())
                || super.mouseClicked(event, doubleClick);
    }

    private boolean clickStep(final CraftRequestButtons buttons, final double x, final double y) {
        for (int i = 0; i < STEPS.length; i++) {
            if (buttons.step(i, STEPS.length).contains(x, y)) {
                setAmount(amount + STEPS[i]);
                return true;
            }
        }
        return false;
    }

    private boolean clickAction(final CraftRequestButtons buttons, final double x, final double y) {
        if (buttons.cancel().contains(x, y)) {
            onClose();
            return true;
        }
        final PlanPreview plan = currentPlan();
        final boolean starts = buttons.start().contains(x, y) && plan != null && plan.isComplete();
        final boolean shrinks = buttons.craftLess().contains(x, y) && canCraftLess(plan);
        if (starts || shrinks) {
            send(starts ? CraftRequest.START : CraftRequest.CRAFT_LESS);
        }
        return starts || shrinks;
    }

    private void setAmount(final long changed) {
        amount = Math.clamp(changed, 1, (long) Integer.MAX_VALUE);
        if (amountBox != null) {
            amountBox.setValue(Long.toString(amount));
        }
        amountChanged();
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final PlanPreview plan = currentPlan();
        final boolean confirms = event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER;
        if (confirms && plan != null && plan.isComplete()) {
            send(CraftRequest.START);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
