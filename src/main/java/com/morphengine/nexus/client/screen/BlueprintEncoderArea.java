package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.blueprint.BlueprintDraft;
import com.morphengine.nexus.blueprint.GridSlot;
import com.morphengine.nexus.client.input.MouseButtonEvent;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.networking.BlueprintAmountPayload;
import com.morphengine.nexus.networking.BlueprintSlotPayload;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.terminal.TerminalLayout;
import com.morphengine.nexus.transfer.FluidResource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Blueprint encoder of a Blueprint Terminal, a row of nine columns under
 * the list. From the left: the button that switches between crafting and
 * processing with the substitutes button below it, the 3 by 3 grid of inputs
 * with the clear button right of its first row, as on a crafting terminal, an
 * arrow, the outputs, and the column of Blueprint slots with the encode button
 * between them. Crafting shows what the grid crafts as
 * its single output; processing lists up to three outputs, and the wheel over
 * a processing slot changes its amount. Slots are ghosts: a click with an
 * item lists it, with an empty hand clears it, and items and fluids can be
 * dragged in from a recipe viewer. A middle click or a Ctrl-click on a
 * processing input moves it on to the next tag its substitutes come from,
 * with Shift back to the previous one; while substitutes count, a mark in its
 * corner shows it has a tag.
 */
final class BlueprintEncoderArea implements TerminalWorkArea {

    private static final int INPUT_COLUMN = 1;
    private static final int ARROW_COLUMN = 4;
    private static final int OUTPUT_COLUMN = 6;
    private static final int ICON_SIZE = 16;
    private static final int HOVER_RGB = 0x80FFFFFF;
    private static final int SHIFT_STEP = 10;
    private static final int ARROW_INSET = 4;

    private final BlueprintTerminalMenu menu;
    private final Font font;
    private final int left;
    private final int top;
    private final BlueprintEncoderButtons buttons;

    BlueprintEncoderArea(final BlueprintTerminalMenu menu, final Font font, final int panelLeft, final int panelTop,
                         final TerminalLayout layout) {
        this.menu = menu;
        this.font = font;
        this.left = panelLeft + layout.encoderLeft();
        this.top = panelTop + layout.craftingTop();
        this.buttons = new BlueprintEncoderButtons(menu, cell(0, 0), cell(0, 1),
                CraftingGridArea.clearButtonOf(panelLeft, panelTop, layout),
                cell(BlueprintTerminalMenu.BLUEPRINT_COLUMN, 1));
    }

    private PanelBounds cell(final int column, final int row) {
        return new PanelBounds(left + column * TerminalLayout.SLOT, top + row * TerminalLayout.SLOT,
                TerminalLayout.SLOT, TerminalLayout.SLOT);
    }

    private PanelBounds slot(final int index) {
        return index < BlueprintDraft.INPUTS
                ? cell(INPUT_COLUMN + index % GridSlot.SIDE, index / GridSlot.SIDE)
                : cell(OUTPUT_COLUMN, index - BlueprintDraft.INPUTS);
    }

    private boolean isCrafting() {
        return menu.draft().kind() == BlueprintKind.CRAFTING;
    }

    private boolean allowsSubstitutes() {
        return menu.draft().substitution().isAllowed();
    }

    /**
     * @return the draft slots the player sets: the grid, and the outputs when processing
     */
    private int editableSlots() {
        return isCrafting() ? BlueprintDraft.INPUTS : BlueprintDraft.SIZE;
    }

    @Override
    public void draw(final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        buttons.draw(graphics, style, mouseX, mouseY);
        for (int index = 0; index < editableSlots(); index++) {
            drawGhost(graphics, style, index, mouseX, mouseY);
        }
        if (isCrafting()) {
            drawCraftingResult(graphics, style);
        }
        final PanelBounds arrowCell = cell(ARROW_COLUMN, 1);
        CraftingGridArea.drawArrow(graphics, arrowCell.left() + ARROW_INSET,
                arrowCell.top() + TerminalLayout.SLOT / 2 - 2, style.border());
    }

    private void drawGhost(final GuiGraphics graphics, final PanelStyle style, final int index,
                           final int mouseX, final int mouseY) {
        final PanelBounds bounds = slot(index);
        style.drawSlot(graphics, bounds.left(), bounds.top());
        final BlueprintDraft.Slot filled = menu.draft().at(index);
        if (filled != null) {
            ResourceRenderers.icon(filled.resource()).draw(graphics, bounds.left() + 1, bounds.top() + 1);
            if (!isCrafting()) {
                SlotAmounts.draw(graphics, font, filled.resource().type().unit().compact(filled.amount()),
                        bounds.left() + 1, bounds.top() + 1);
            }
            if (filled.tag() != null && allowsSubstitutes()) {
                style.drawTagMark(graphics, bounds.left(), bounds.top());
            }
        }
        if (bounds.contains(mouseX, mouseY)) {
            graphics.fill(bounds.left() + 1, bounds.top() + 1, bounds.left() + 1 + ICON_SIZE,
                    bounds.top() + 1 + ICON_SIZE, HOVER_RGB);
        }
    }

    /**
     * What the grid crafts, in the middle output slot, which the player does not set.
     */
    private void drawCraftingResult(final GuiGraphics graphics, final PanelStyle style) {
        final PanelBounds bounds = cell(OUTPUT_COLUMN, 1);
        style.drawSlot(graphics, bounds.left(), bounds.top());
        final List<ResourceAmount> outputs = menu.craftingOutputs();
        if (!outputs.isEmpty()) {
            final ResourceAmount result = outputs.getFirst();
            final NexusResource resource = NexusResources.of(result.resource());
            ResourceRenderers.icon(resource).draw(graphics, bounds.left() + 1, bounds.top() + 1);
            SlotAmounts.draw(graphics, font, resource.type().unit().compact(result.amount()), bounds.left() + 1,
                    bounds.top() + 1);
        }
    }

    @Override
    public List<Component> tooltip(final int mouseX, final int mouseY) {
        final List<Component> button = buttons.tooltip(mouseX, mouseY);
        if (!button.isEmpty()) {
            return button;
        }
        if (isCrafting() && cell(OUTPUT_COLUMN, 1).contains(mouseX, mouseY)) {
            return outputsTooltip();
        }
        return slotTooltip(slotAt(mouseX, mouseY));
    }

    private List<Component> outputsTooltip() {
        final List<Component> lines = new ArrayList<>();
        for (ResourceAmount output : menu.craftingOutputs()) {
            final NexusResource resource = NexusResources.of(output.resource());
            lines.add(Component.translatable("tooltip.nexus.blueprint.entry",
                    resource.type().unit().quantity(output.amount()), resource.name()));
        }
        return lines;
    }

    private List<Component> slotTooltip(final int index) {
        final BlueprintDraft.Slot filled = index < 0 ? null : menu.draft().at(index);
        if (filled == null || !menu.getCarried().isEmpty()) {
            return List.of();
        }
        final List<Component> lines = new ArrayList<>(ResourceRenderers.tooltip(filled.resource()));
        if (!isCrafting()) {
            lines.add(filled.resource().type().unit().quantity(filled.amount()).withStyle(ChatFormatting.GRAY));
            if (index < BlueprintDraft.INPUTS) {
                lines.add(tagLine(filled));
            }
            lines.add(Component.translatable("gui.nexus.blueprint.amount_hint").withStyle(ChatFormatting.DARK_GRAY));
            if (index < BlueprintDraft.INPUTS) {
                lines.add(Component.translatable("gui.nexus.blueprint.tag_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return lines;
    }

    /**
     * What substitutes a processing input takes: the members of its tag, or
     * none; greyed out while the draft takes no substitutes at all.
     */
    private Component tagLine(final BlueprintDraft.Slot input) {
        final ResourceLocation tag = input.tag();
        final MutableComponent line = tag == null ? Component.translatable("gui.nexus.blueprint.tag.none")
                : Component.translatable("gui.nexus.blueprint.tag", "#" + tag,
                        input.resource().membersOf(tag).size());
        if (!allowsSubstitutes()) {
            line.append(Component.translatable("gui.nexus.blueprint.tag.off"));
        }
        return line.withStyle(allowsSubstitutes() ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_GRAY);
    }

    private int slotAt(final double x, final double y) {
        for (int index = 0; index < editableSlots(); index++) {
            if (slot(index).contains(x, y)) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public boolean click(final MouseButtonEvent event) {
        if (buttons.click(event.x(), event.y())) {
            return true;
        }
        final int index = slotAt(event.x(), event.y());
        if (index < 0) {
            return cell(OUTPUT_COLUMN, 1).contains(event.x(), event.y());
        }
        clickSlot(event, index);
        return true;
    }

    /**
     * An item lists what it is or holds; an empty hand clears the slot or, on
     * a processing input, moves it on to another tag.
     */
    private void clickSlot(final MouseButtonEvent event, final int index) {
        final ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            final NexusResource resource = event.hasShiftDown() ? ghostEntryOf(carried) : contentsOf(carried);
            if (resource != null) {
                send(index, resource);
            }
        } else if (hasTag(index) && FilterGrid.asksForNextTag(event)) {
            buttons.press(event.hasShiftDown() ? BlueprintTerminalMenu.BUTTON_PREVIOUS_TAG + index
                    : BlueprintTerminalMenu.BUTTON_NEXT_TAG + index);
        } else {
            send(index, null);
        }
    }

    /**
     * @return whether draft slot {@code index} is a filled processing input, which has a tag to pick
     */
    private boolean hasTag(final int index) {
        return !isCrafting() && index < BlueprintDraft.INPUTS && menu.draft().at(index) != null;
    }

    /**
     * What a click with {@code stack} lists: when processing, the fluid a filled
     * container holds; otherwise the item.
     */
    private @Nullable NexusResource contentsOf(final ItemStack stack) {
        final NexusResource fluid = isCrafting() ? null
                : ghostEntryOf(FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY));
        return fluid != null ? fluid : ghostEntryOf(stack);
    }

    @Override
    public boolean scroll(final double x, final double y, final double amount) {
        final int index = slotAt(x, y);
        final BlueprintDraft.Slot filled = index < 0 ? null : menu.draft().at(index);
        if (filled == null || isCrafting() || amount == 0) {
            return false;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final long steps = Screen.hasShiftDown() ? SHIFT_STEP : 1;
        final long step = steps * filled.resource().type().unit().step() * (long) Math.signum(amount);
        PacketDistributor.sendToServer(new BlueprintAmountPayload(menu.containerId, index,
                filled.amount() + step));
        return true;
    }

    @Override
    public List<Rect2i> ghostAreas() {
        final List<Rect2i> areas = new ArrayList<>(editableSlots());
        for (int index = 0; index < editableSlots(); index++) {
            areas.add(slot(index).toRect());
        }
        return areas;
    }

    @Override
    public @Nullable NexusResource ghostEntryOf(final ItemStack stack) {
        return stack.isEmpty() ? null : ItemKey.of(stack);
    }

    @Override
    public @Nullable NexusResource ghostEntryOf(final FluidStack fluid) {
        return isCrafting() || fluid.isEmpty() ? null : new FluidKey(FluidResource.of(fluid));
    }

    @Override
    public void setGhost(final int slot, final NexusResource resource) {
        send(slot, resource);
    }

    private void send(final int index, final @Nullable NexusResource resource) {
        PacketDistributor.sendToServer(new BlueprintSlotPayload(menu.containerId, index, resource));
    }
}
