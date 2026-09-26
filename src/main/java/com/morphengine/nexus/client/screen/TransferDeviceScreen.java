package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.menu.TransferDeviceMenu;
import com.morphengine.nexus.networking.FilterModePayload;
import com.morphengine.nexus.networking.KeepAmountPayload;
import com.morphengine.nexus.resource.AmountUnit;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.transfer.TransferSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Puller or Pusher panel, tinted with its network's color: the network, the
 * filter, the upgrade slots right of it and the inventory, with the mode
 * buttons in a column left of the panel. The order and amounts of delivery
 * show only on a Pusher with a whitelist, the only one they apply to; when it
 * keeps amounts stocked, each filter slot shows its amount, changed with the
 * mouse wheel.
 */
public final class TransferDeviceScreen extends PanelScreen<TransferDeviceMenu> implements FilterScreen {

    private static final int IMAGE_WIDTH = 224;
    private static final int IMAGE_HEIGHT = TransferDeviceMenu.INVENTORY_TOP + 84;
    private static final int FILTER_LABEL_TOP = TransferDeviceMenu.FILTER_TOP - 12;
    private static final int LABEL_GAP = 11;
    private static final int SHIFT_STEP = 10;

    private FilterGrid<TransferDeviceMenu> filterGrid;

    public TransferDeviceScreen(final TransferDeviceMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.filterGrid = createFilterGrid();
    }

    @Override
    protected void init() {
        super.init();
        filterGrid = createFilterGrid();
    }

    private FilterGrid<TransferDeviceMenu> createFilterGrid() {
        return new FilterGrid<>(getMenu(), leftPos + TransferDeviceMenu.FILTER_LEFT,
                topPos + TransferDeviceMenu.FILTER_TOP, TransferDeviceMenu.FILTER_COLUMNS);
    }

    /**
     * @return the mode buttons the panel shows now, top to bottom
     */
    private List<Control> controls() {
        return Control.shownBy(getMenu());
    }

    private SideButtons sideButtons() {
        return new SideButtons(leftPos, topPos, controls().size());
    }

    /**
     * @return the column of mode buttons left of the panel, for a recipe viewer to keep clear of
     */
    public Rect2i sidebarArea() {
        return sideButtons().area().toRect();
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().badge());
    }

    @Override
    protected boolean isTitleEditable() {
        return false;
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final NetworkBadge network = getMenu().badge();
        PanelStyle.drawNetwork(graphics, font, network, leftPos, topPos);
        graphics.text(font, Component.translatable(getMenu().deliversListed()
                        ? "gui.nexus.transfer.deliver" : "gui.nexus.filter"),
                leftPos + PanelStyle.PADDING, topPos + FILTER_LABEL_TOP, PanelStyle.TEXT_DIM, false);
        filterGrid.draw(graphics, style, mouseX, mouseY);
        if (getMenu().showsKeepAmounts()) {
            drawKeepAmounts(graphics);
        }
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        drawSideButtons(graphics, style, mouseX, mouseY);
        graphics.text(font, playerInventoryTitle, leftPos + TransferDeviceMenu.INVENTORY_LEFT,
                topPos + TransferDeviceMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    private void drawKeepAmounts(final GuiGraphicsExtractor graphics) {
        final TransferSettings settings = getMenu().settings();
        for (FilterSlots.Entry entry : settings.filter().entries()) {
            final PanelBounds slot = filterGrid.slot(entry.slot());
            final long amount = settings.keepAmount(entry.slot(), entry.resource());
            SlotAmounts.draw(graphics, font, entry.resource().type().unit().compact(amount),
                    slot.left() + 1, slot.top() + 1);
        }
    }

    private void drawSideButtons(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final List<Control> controls = controls();
        final SideButtons buttons = sideButtons();
        final int hovered = buttons.buttonAt(mouseX, mouseY);
        final TransferSettings settings = getMenu().settings();
        for (int index = 0; index < controls.size(); index++) {
            buttons.draw(graphics, style, index, controls.get(index).icon(settings), index == hovered);
        }
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        final List<Component> lines = tooltipAt(mouseX, mouseY);
        if (!lines.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private List<Component> tooltipAt(final int mouseX, final int mouseY) {
        final int button = sideButtons().buttonAt(mouseX, mouseY);
        if (button >= 0) {
            return controls().get(button).tooltip(getMenu().settings());
        }
        final int slot = filterGrid.slotAt(mouseX, mouseY);
        final NexusResource resource = filterGrid.resourceAt(mouseX, mouseY);
        if (resource == null || !getMenu().getCarried().isEmpty()) {
            return List.of();
        }
        final List<Component> lines = new ArrayList<>(ResourceRenderers.tooltip(resource));
        if (getMenu().showsKeepAmounts()) {
            final AmountUnit unit = resource.type().unit();
            lines.add(Component.translatable("gui.nexus.transfer.keep_amount",
                    unit.exact(getMenu().settings().keepAmount(slot, resource))).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final int button = sideButtons().buttonAt(event.x(), event.y());
        if (button >= 0) {
            press(controls().get(button), event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            return true;
        }
        return filterGrid.click(event) || super.mouseClicked(event, doubleClick);
    }

    private void press(final Control control, final boolean backwards) {
        if (control == Control.FILTER_MODE) {
            ClientPacketDistributor.sendToServer(new FilterModePayload(getMenu().containerId));
        } else if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId,
                    control.buttonId + (backwards ? 1 : 0));
        }
    }

    /**
     * Over a filter slot of a Pusher that keeps amounts stocked, the wheel
     * changes that slot's amount by one whole of its resource, or ten with Shift.
     */
    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        final int slot = filterGrid.slotAt(x, y);
        final NexusResource resource = slot < 0 ? null : getMenu().filter().resourceAt(slot);
        if (resource == null || !getMenu().showsKeepAmounts() || scrollY == 0) {
            return super.mouseScrolled(x, y, scrollX, scrollY);
        }
        final long wholes = minecraft != null && minecraft.hasShiftDown() ? SHIFT_STEP : 1;
        final long step = wholes * resource.type().unit().unitsPerWhole() * (long) Math.signum(scrollY);
        final long current = getMenu().settings().keepAmount(slot, resource);
        ClientPacketDistributor.sendToServer(new KeepAmountPayload(getMenu().containerId, slot, current + step));
        return true;
    }

    @Override
    public List<Rect2i> filterSlotAreas() {
        return filterGrid.areas();
    }

    @Override
    public @Nullable NexusResource filterEntryOf(final ItemStack stack) {
        return filterGrid.entryOf(stack);
    }

    @Override
    public @Nullable NexusResource filterEntryOf(final FluidStack fluid) {
        return filterGrid.entryOf(fluid);
    }

    @Override
    public void setFilterSlot(final int slot, final NexusResource resource) {
        filterGrid.send(slot, resource);
    }

    /**
     * A mode button of the panel.
     */
    private enum Control {
        FILTER_MODE(-1),
        REDSTONE(TransferDeviceMenu.BUTTON_REDSTONE),
        SCHEDULING(TransferDeviceMenu.BUTTON_SCHEDULING),
        DELIVERY(TransferDeviceMenu.BUTTON_DELIVERY);

        private static final List<Control> COMMON = List.of(FILTER_MODE, REDSTONE);
        private static final List<Control> DELIVERING = List.of(FILTER_MODE, REDSTONE, SCHEDULING, DELIVERY);

        private final int buttonId;
        private final String key = name().toLowerCase(Locale.ROOT);

        Control(final int buttonId) {
            this.buttonId = buttonId;
        }

        static List<Control> shownBy(final TransferDeviceMenu menu) {
            return menu.deliversListed() ? DELIVERING : COMMON;
        }

        /**
         * @return the button's name, the choice made, and for the order and
         *         amount of delivery what that choice does
         */
        List<Component> tooltip(final TransferSettings settings) {
            final String prefix = this == FILTER_MODE ? "gui.nexus.filter" : "gui.nexus.transfer." + key;
            final List<Component> lines = new ArrayList<>(3);
            lines.add(Component.translatable(prefix));
            lines.add(Component.translatable(prefix + "." + choiceName(settings)).withStyle(ChatFormatting.GRAY));
            if (this == SCHEDULING || this == DELIVERY) {
                lines.add(Component.translatable(prefix + "." + choiceName(settings) + ".hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return lines;
        }

        Identifier icon(final TransferSettings settings) {
            return Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "transfer/" + key + "_" + choiceName(settings));
        }

        private String choiceName(final TransferSettings settings) {
            return switch (this) {
                case FILTER_MODE -> settings.filter().mode() == FilterMode.ALLOW ? "whitelist" : "blacklist";
                case REDSTONE -> settings.redstone().name().toLowerCase(Locale.ROOT);
                case SCHEDULING -> settings.scheduling().name().toLowerCase(Locale.ROOT);
                case DELIVERY -> settings.delivery().getSerializedName();
            };
        }
    }
}
