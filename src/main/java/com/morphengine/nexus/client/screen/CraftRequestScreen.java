package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asks the network to craft a resource, over the terminal it was opened from:
 * the amount, typed or stepped with the buttons, and the plan for it, worked
 * out by the server a moment after the amount last changed. Each resource of
 * the plan shows what is taken from storage, what is crafted and what is
 * missing. Start begins the task when nothing is missing and goes back to the
 * terminal; so do Escape and Cancel, without starting anything.
 *
 * @param <M> the terminal's menu, which stays open underneath
 */
final class CraftRequestScreen<M extends AbstractContainerMenu & TerminalPanel> extends Screen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 222;
    private static final int AMOUNT_TOP = 22;
    private static final int STEPS_TOP = 42;
    private static final int PLAN_TOP = 62;
    private static final int ROW_HEIGHT = 18;
    private static final int ROWS = 7;
    private static final int BUTTON_TOP = HEIGHT - 22;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 14;
    private static final int STEP_WIDTH = 30;
    private static final int STEP_GAP = 2;
    private static final int[] STEPS = {1, 10, 64, -1, -10, -64};
    private static final int PREVIEW_DELAY_TICKS = 5;
    private static final int MISSING_RGB = 0xFFE05A4E;
    private static final int CRAFTED_RGB = 0xFF7FC7FF;
    private static final int MAX_DIGITS = 9;
    private static final int AMOUNT_LEFT = PanelStyle.PADDING + 22;
    private static final int AMOUNT_WIDTH = 80;
    private static final int TEXT_INSET = 4;
    private static final int TEXT_GAP = 6;
    /** Columns of a row of the plan: taken from storage, crafted, missing. */
    private static final int FROM_STORAGE = 0;
    private static final int CRAFTED = 1;
    private static final int MISSING = 2;
    private static final int COLUMNS = 3;

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

    @Override
    protected void init() {
        final EditBox box = new EditBox(font, left() + AMOUNT_LEFT, top() + AMOUNT_TOP, AMOUNT_WIDTH,
                BUTTON_HEIGHT, Component.translatable("gui.nexus.craft.amount"));
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
            }
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
        final PanelBounds panel = new PanelBounds(left(), top(), WIDTH, HEIGHT);
        style.drawFrame(graphics, font, panel, title);
        ResourceRenderers.icon(resource).draw(graphics, left() + PanelStyle.PADDING, top() + AMOUNT_TOP - 1);
        for (int i = 0; i < STEPS.length; i++) {
            style.drawButton(graphics, font, stepButton(i), Component.literal((STEPS[i] > 0 ? "+" : "") + STEPS[i]));
        }
        drawPlan(graphics, style);
        style.drawButton(graphics, font, cancelButton(), Component.translatable("gui.nexus.craft.cancel"));
        final PlanPreview plan = currentPlan();
        style.drawButton(graphics, font, startButton(), Component.translatable(plan != null && plan.isComplete()
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
        final List<Row> rows = rowsOf(plan);
        scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - ROWS));
        for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
            final int y = top() + PLAN_TOP + i * ROW_HEIGHT;
            style.drawSlot(graphics, rowsLeft, y);
            drawRow(graphics, rows.get(scroll + i), rowsLeft, y);
        }
    }

    private void drawRow(final GuiGraphicsExtractor graphics, final Row row, final int x, final int y) {
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

    private static List<Row> rowsOf(final PlanPreview plan) {
        final Map<ResourceKey, long[]> amounts = new LinkedHashMap<>();
        collect(amounts, plan.missing(), MISSING);
        collect(amounts, plan.crafted(), CRAFTED);
        collect(amounts, plan.fromStorage(), FROM_STORAGE);
        final List<Row> rows = new ArrayList<>(amounts.size());
        for (Map.Entry<ResourceKey, long[]> entry : amounts.entrySet()) {
            final long[] values = entry.getValue();
            rows.add(new Row(entry.getKey(), values[FROM_STORAGE], values[CRAFTED], values[MISSING]));
        }
        return rows;
    }

    private static void collect(final Map<ResourceKey, long[]> amounts, final List<ResourceAmount> list,
                                final int column) {
        for (ResourceAmount amount : list) {
            amounts.computeIfAbsent(amount.resource(), key -> new long[COLUMNS])[column] += amount.amount();
        }
    }

    private PanelBounds stepButton(final int index) {
        final int rowWidth = STEPS.length * (STEP_WIDTH + STEP_GAP) - STEP_GAP;
        return new PanelBounds(left() + (WIDTH - rowWidth) / 2 + index * (STEP_WIDTH + STEP_GAP), top() + STEPS_TOP,
                STEP_WIDTH, BUTTON_HEIGHT);
    }

    private PanelBounds cancelButton() {
        return new PanelBounds(left() + PanelStyle.PADDING, top() + BUTTON_TOP, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private PanelBounds startButton() {
        return new PanelBounds(left() + WIDTH - PanelStyle.PADDING - BUTTON_WIDTH, top() + BUTTON_TOP,
                BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        for (int i = 0; i < STEPS.length; i++) {
            if (stepButton(i).contains(event.x(), event.y())) {
                setAmount(amount + STEPS[i]);
                return true;
            }
        }
        if (cancelButton().contains(event.x(), event.y())) {
            onClose();
            return true;
        }
        final PlanPreview plan = currentPlan();
        if (startButton().contains(event.x(), event.y()) && plan != null && plan.isComplete()) {
            send(CraftRequest.START);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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

    /**
     * One resource of the plan: units taken from storage, crafted and missing.
     */
    private record Row(ResourceKey resource, long fromStorage, long crafted, long missing) {
    }
}
