package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.automation.TaskEntry;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.client.input.MouseButtonEvent;
import com.morphengine.nexus.menu.CraftingMonitorMenu;
import com.morphengine.nexus.networking.CancelTaskPayload;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Crafting Monitor panel, tinted with its network's color: the network's
 * crafting tasks on the left, each with its product, amount and progress, and
 * the task picked on the right: every resource it holds, crafts or waits for,
 * and what holds it up, with a button to cancel it. A resource shows its
 * amounts as bare numbers in the colors of the legend above the list, as far
 * as they fit; its tooltip names them all.
 */
public final class CraftingMonitorScreen extends PanelScreen<CraftingMonitorMenu> {

    private static final int IMAGE_WIDTH = 276;
    private static final int IMAGE_HEIGHT = 232;
    private static final int LIST_TOP = 34;
    private static final int LIST_WIDTH = 104;
    private static final int TASK_HEIGHT = 26;
    private static final int TASK_ROWS = 7;
    private static final int DETAIL_LEFT = PanelStyle.PADDING + LIST_WIDTH + 8;
    private static final int ENTRY_HEIGHT = 18;
    private static final int ENTRY_ROWS = 7;
    private static final int ENTRIES_TOP = LIST_TOP + 34;
    private static final int BAR_HEIGHT = 2;
    private static final int PERCENT = 100;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 14;
    private static final int TEXT_GAP = 6;
    private static final int PROBLEM_RGB = 0xFFE05A4E;
    private static final int PROCESSING_RGB = 0xFFE3B35A;
    private static final int SCHEDULED_RGB = 0xFF7FC7FF;
    private static final int SELECTED_RGB = 0x40FFFFFF;
    /** Text inside a box sits this far below its top edge. */
    private static final int TEXT_INSET = 4;
    private static final int TASK_TEXT_LEFT = 20;
    private static final int TASK_AMOUNT_TOP = 2;
    private static final int TASK_STATE_TOP = 11;
    /** The progress bar leaves this many rows free above the bottom edge of a task. */
    private static final int BAR_INSET = 2;
    private static final int LINE = 10;
    private static final int PROBLEM_DOT = 3;
    private static final int PROBLEM_DOT_INSET = 5;
    private static final int BOTTOM_PADDING = 8;

    private @Nullable UUID selected;
    private int taskScroll;
    private int entryScroll;

    public CraftingMonitorScreen(final CraftingMonitorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().badge());
    }

    private @Nullable TaskStatus selectedTask() {
        final List<TaskStatus> tasks = getMenu().tasks();
        for (TaskStatus task : tasks) {
            if (task.id().equals(selected)) {
                return task;
            }
        }
        return tasks.isEmpty() ? null : tasks.getFirst();
    }

    @Override
    protected void extractPanel(
            final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        PanelStyle.drawNetwork(graphics, font, getMenu().badge(), leftPos, topPos);
        final List<TaskStatus> tasks = getMenu().tasks();
        if (tasks.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nexus.monitor.empty"), leftPos + PanelStyle.PADDING,
                    topPos + LIST_TOP + TEXT_INSET, PanelStyle.TEXT_DIM, false);
            return;
        }
        taskScroll = Math.clamp(taskScroll, 0, Math.max(0, tasks.size() - TASK_ROWS));
        final TaskStatus shown = selectedTask();
        for (int row = 0; row < TASK_ROWS && taskScroll + row < tasks.size(); row++) {
            final TaskStatus task = tasks.get(taskScroll + row);
            drawTask(graphics, style, task, taskRow(row), task == shown);
        }
        if (shown != null) {
            drawDetails(graphics, style, shown);
        }
    }

    private PanelBounds taskRow(final int row) {
        return new PanelBounds(leftPos + PanelStyle.PADDING, topPos + LIST_TOP + row * TASK_HEIGHT, LIST_WIDTH,
                TASK_HEIGHT - 2);
    }

    private void drawTask(final GuiGraphics graphics, final PanelStyle style, final TaskStatus task,
                          final PanelBounds row, final boolean isShown) {
        graphics.fill(row.left(), row.top(), row.left() + row.width(), row.top() + row.height(), style.buttonFill());
        if (isShown) {
            graphics.fill(row.left(), row.top(), row.left() + row.width(), row.top() + row.height(), SELECTED_RGB);
        }
        graphics.renderOutline(row.left(), row.top(), row.width(), row.height(), style.border());
        final NexusResource product = NexusResources.of(task.target().resource());
        ResourceRenderers.icon(product).draw(graphics, row.left() + 2, row.top() + 1);
        final String amount = product.type().unit().compact(task.target().amount());
        graphics.drawString(font, amount, row.left() + TASK_TEXT_LEFT, row.top() + TASK_AMOUNT_TOP,
                PanelStyle.TEXT_LIGHT,
                false);
        final Component state = Component.translatable(
                "gui.nexus.monitor.state." + task.state().name().toLowerCase(Locale.ROOT));
        graphics.drawString(font, state, row.left() + TASK_TEXT_LEFT, row.top() + TASK_STATE_TOP, PanelStyle.TEXT_DIM,
                false);
        final int barLeft = row.left() + 2;
        final int barWidth = row.width() - 4;
        final int barTop = row.top() + row.height() - BAR_HEIGHT - BAR_INSET;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + BAR_HEIGHT, style.track());
        graphics.fill(barLeft, barTop, barLeft + (int) (barWidth * task.progress()), barTop + BAR_HEIGHT,
                style.accent());
    }

    private void drawDetails(final GuiGraphics graphics, final PanelStyle style, final TaskStatus task) {
        final int left = leftPos + DETAIL_LEFT;
        final NexusResource product = NexusResources.of(task.target().resource());
        graphics.drawString(font, product.name(), left, topPos + LIST_TOP, PanelStyle.TEXT_LIGHT, false);
        final MutableComponent progress = Component.translatable("gui.nexus.monitor.progress",
                Math.round(task.progress() * PERCENT));
        if (!task.requester().isEmpty()) {
            progress.append(" · ").append(task.requester());
        }
        graphics.drawString(font, progress, left, topPos + LIST_TOP + LINE, PanelStyle.TEXT_DIM, false);
        drawLegend(graphics, left, topPos + LIST_TOP + 2 * LINE);
        final List<TaskEntry> entries = task.entries();
        entryScroll = Math.clamp(entryScroll, 0, Math.max(0, entries.size() - ENTRY_ROWS));
        for (int row = 0; row < ENTRY_ROWS && entryScroll + row < entries.size(); row++) {
            final int y = topPos + ENTRIES_TOP + row * ENTRY_HEIGHT;
            style.drawSlot(graphics, left, y);
            drawEntry(graphics, entries.get(entryScroll + row), left, y);
        }
        style.drawButton(graphics, font, cancelButton(), Component.translatable("gui.nexus.monitor.cancel"));
    }

    /**
     * What the colors of the amounts mean, each word in its own color.
     */
    private void drawLegend(final GuiGraphics graphics, final int left, final int top) {
        int column = left;
        for (Amount amount : Amount.values()) {
            column = drawFitting(graphics, Component.translatable(amount.legendKey), column, top, amount.color);
        }
    }

    private void drawEntry(final GuiGraphics graphics, final TaskEntry entry, final int x, final int y) {
        final NexusResource resource = NexusResources.of(entry.resource());
        ResourceRenderers.icon(resource).draw(graphics, x + 1, y + 1);
        int column = x + PanelStyle.SLOT_SIZE + TEXT_INSET;
        final int textTop = y + (PanelStyle.SLOT_SIZE - font.lineHeight) / 2 + 1;
        for (Amount amount : Amount.values()) {
            final long value = amount.of(entry);
            if (value > 0) {
                column = drawFitting(graphics, Component.literal(resource.type().unit().compact(value)), column,
                        textTop, amount.color);
            }
        }
        if (entry.problem() != DispatchResult.ACCEPTED) {
            final int dot = x + PanelStyle.SLOT_SIZE - PROBLEM_DOT_INSET;
            graphics.fill(dot, y + 2, dot + PROBLEM_DOT, y + 2 + PROBLEM_DOT, PROBLEM_RGB);
        }
    }

    /**
     * Draws {@code text} at {@code x} only when it ends inside the panel.
     *
     * @return where the next text starts; past the panel's edge once one did not fit
     */
    private int drawFitting(final GuiGraphics graphics, final Component text, final int x, final int y,
                            final int color) {
        final int end = x + font.width(text);
        if (end > leftPos + IMAGE_WIDTH - PanelStyle.PADDING) {
            return leftPos + IMAGE_WIDTH;
        }
        graphics.drawString(font, text, x, y, color, false);
        return end + TEXT_GAP;
    }

    private PanelBounds cancelButton() {
        return new PanelBounds(leftPos + IMAGE_WIDTH - PanelStyle.PADDING - BUTTON_WIDTH,
                topPos + IMAGE_HEIGHT - BUTTON_HEIGHT - BOTTOM_PADDING, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        final List<Component> lines = entryTooltip(mouseX, mouseY);
        if (!lines.isEmpty()) {
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private List<Component> entryTooltip(final int mouseX, final int mouseY) {
        final TaskStatus task = selectedTask();
        final int row = (mouseY - topPos - ENTRIES_TOP) / ENTRY_HEIGHT;
        final boolean inColumn = mouseX >= leftPos + DETAIL_LEFT && mouseX < leftPos + IMAGE_WIDTH - PanelStyle.PADDING
                && mouseY >= topPos + ENTRIES_TOP && row < ENTRY_ROWS;
        if (task == null || !inColumn || entryScroll + row >= task.entries().size()) {
            return List.of();
        }
        final TaskEntry entry = task.entries().get(entryScroll + row);
        final NexusResource resource = NexusResources.of(entry.resource());
        final List<Component> lines = new ArrayList<>(ResourceRenderers.tooltip(resource));
        for (Amount amount : Amount.values()) {
            final long value = amount.of(entry);
            if (value > 0) {
                lines.add(Component.translatable(amount.key, resource.type().unit().compact(value))
                        .withColor(amount.color));
            }
        }
        if (entry.problem() != DispatchResult.ACCEPTED) {
            lines.add(Component.translatable("gui.nexus.monitor.problem."
                    + entry.problem().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        final List<TaskStatus> tasks = getMenu().tasks();
        for (int row = 0; row < TASK_ROWS && taskScroll + row < tasks.size(); row++) {
            if (taskRow(row).contains(event.x(), event.y())) {
                selected = tasks.get(taskScroll + row).id();
                entryScroll = 0;
                return true;
            }
        }
        final TaskStatus task = selectedTask();
        if (task != null && cancelButton().contains(event.x(), event.y())) {
            PacketDistributor.sendToServer(new CancelTaskPayload(getMenu().containerId, task.id()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        final int step = (int) Math.signum(scrollY);
        if (x < leftPos + DETAIL_LEFT) {
            taskScroll -= step;
        } else {
            entryScroll -= step;
        }
        return true;
    }

    /**
     * The amounts a resource of a task shows, in the order shown, with their colors.
     */
    private enum Amount {

        HELD("gui.nexus.monitor.held", PanelStyle.TEXT_LIGHT),
        SCHEDULED("gui.nexus.monitor.scheduled", SCHEDULED_RGB),
        PROCESSING("gui.nexus.monitor.processing", PROCESSING_RGB);

        private final String key;
        private final String legendKey;
        private final int color;

        Amount(final String key, final int color) {
            this.key = key;
            this.legendKey = key + ".legend";
            this.color = color;
        }

        long of(final TaskEntry entry) {
            return switch (this) {
                case HELD -> entry.held();
                case SCHEDULED -> entry.scheduled();
                case PROCESSING -> entry.processing();
            };
        }
    }
}
