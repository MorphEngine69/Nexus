package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.storage.CellUsage;
import com.morphengine.nexus.item.CellFilter;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.menu.VaultCellMenu;
import com.morphengine.nexus.networking.CellFilterModePayload;
import com.morphengine.nexus.networking.CellFilterPayload;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Panel of the Vault Cell in hand: how full it is, its filter and its name.
 * A filter slot clicked with an item lists that item; for a fluid cell, a
 * bucket or tank lists the fluid inside. An empty cursor or a right click
 * clears the slot. A recipe viewer can drop items and fluids on the filter
 * slots too. Nothing is taken from the player, and nothing is given.
 */
public final class VaultCellScreen extends PanelScreen<VaultCellMenu> {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = VaultCellMenu.INVENTORY_TOP + 84;
    private static final int PADDING = 10;
    private static final int USAGE_TOP = 22;
    private static final int LINE_HEIGHT = 11;
    private static final int FILTER_LABEL_TOP = 50;
    private static final int MODE_WIDTH = 72;
    private static final int MODE_HEIGHT = 14;
    private static final int LABEL_GAP = 11;
    private static final int HOVER_RGB = 0x80FFFFFF;
    private static final int ICON_SIZE = 16;

    public VaultCellScreen(final VaultCellMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.tinted(NetworkColor.DEFAULT.rgb());
    }

    private PanelBounds modeButton() {
        return new PanelBounds(leftPos + IMAGE_WIDTH - PADDING - MODE_WIDTH,
                topPos + FILTER_LABEL_TOP - (MODE_HEIGHT - font.lineHeight) / 2 - 1, MODE_WIDTH, MODE_HEIGHT);
    }

    private PanelBounds filterSlot(final int slot) {
        return new PanelBounds(leftPos + VaultCellMenu.FILTER_LEFT + slot * PanelStyle.SLOT_SIZE,
                topPos + VaultCellMenu.FILTER_TOP, PanelStyle.SLOT_SIZE, PanelStyle.SLOT_SIZE);
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final VaultCellItem item = getMenu().cellItem();
        if (item == null) {
            return;
        }
        final CellUsage usage = item.openStorage(getMenu().cell()).usage();
        final int x = leftPos + PADDING;
        graphics.text(font, Component.translatable("tooltip.nexus.cell.bytes", grouped(usage.usedBytes()),
                grouped(usage.totalBytes())), x, topPos + USAGE_TOP, PanelStyle.TEXT_LIGHT, false);
        graphics.text(font, Component.translatable("tooltip.nexus.cell.types", usage.storedTypes(),
                usage.maxTypes()), x, topPos + USAGE_TOP + LINE_HEIGHT, PanelStyle.TEXT_LIGHT, false);
        graphics.text(font, Component.translatable("gui.nexus.cell.filter"), x, topPos + FILTER_LABEL_TOP,
                PanelStyle.TEXT_DIM, false);
        final CellFilter filter = getMenu().filter();
        style.drawButton(graphics, font, modeButton(), Component.translatable(filter.mode() == FilterMode.ALLOW
                ? "gui.nexus.cell.whitelist" : "gui.nexus.cell.blacklist"));
        drawFilter(graphics, style, filter, mouseX, mouseY);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        graphics.text(font, playerInventoryTitle, leftPos + VaultCellMenu.INVENTORY_LEFT,
                topPos + VaultCellMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    private void drawFilter(final GuiGraphicsExtractor graphics, final PanelStyle style, final CellFilter filter,
                            final int mouseX, final int mouseY) {
        for (int slot = 0; slot < CellFilter.SLOTS; slot++) {
            final PanelBounds bounds = filterSlot(slot);
            style.drawSlot(graphics, bounds.left(), bounds.top());
            final NexusResource resource = filter.resourceAt(slot);
            if (resource != null) {
                ResourceRenderers.icon(resource).draw(graphics, bounds.left() + 1, bounds.top() + 1);
            }
            if (bounds.contains(mouseX, mouseY)) {
                graphics.fill(bounds.left() + 1, bounds.top() + 1, bounds.left() + 1 + ICON_SIZE,
                        bounds.top() + 1 + ICON_SIZE, HOVER_RGB);
            }
        }
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!getMenu().getCarried().isEmpty()) {
            return;
        }
        for (int slot = 0; slot < CellFilter.SLOTS; slot++) {
            final NexusResource resource = getMenu().filter().resourceAt(slot);
            if (resource != null && filterSlot(slot).contains(mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(font, ResourceRenderers.tooltip(resource), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (modeButton().contains(event.x(), event.y())) {
            ClientPacketDistributor.sendToServer(new CellFilterModePayload(getMenu().containerId));
            return true;
        }
        for (int slot = 0; slot < CellFilter.SLOTS; slot++) {
            if (filterSlot(slot).contains(event.x(), event.y())) {
                clickFilterSlot(slot, event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Lists what the cursor carries, or clears the slot for an empty cursor or a
     * right click. Something the cell cannot store changes nothing.
     */
    private void clickFilterSlot(final int slot, final boolean clear) {
        final ItemStack carried = getMenu().getCarried();
        final NexusResource resource = clear ? null : filterEntryOf(carried);
        if (resource == null && !clear && !carried.isEmpty()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new CellFilterPayload(getMenu().containerId, slot, resource));
    }

    /**
     * @return where the filter slots are on the screen, in slot order, for a
     *         recipe viewer to drop resources on
     */
    public List<Rect2i> filterSlotAreas() {
        final List<Rect2i> areas = new ArrayList<>(CellFilter.SLOTS);
        for (int slot = 0; slot < CellFilter.SLOTS; slot++) {
            areas.add(filterSlot(slot).toRect());
        }
        return areas;
    }

    /**
     * @return what {@code stack} lists in a filter slot of this cell: the item
     *         for an item cell, the fluid inside for a fluid cell; {@code null}
     *         when it lists nothing here
     */
    public @Nullable NexusResource filterEntryOf(final ItemStack stack) {
        final VaultCellItem item = getMenu().cellItem();
        if (stack.isEmpty() || item == null) {
            return null;
        }
        return item.kind() == CellKind.ITEM
                ? ItemKey.of(stack) : filterEntryOf(FluidUtil.getFirstStackContained(stack));
    }

    /**
     * @return {@code fluid} as a filter entry of this cell; {@code null} for an
     *         item cell or an empty stack
     */
    public @Nullable NexusResource filterEntryOf(final FluidStack fluid) {
        final VaultCellItem item = getMenu().cellItem();
        if (fluid.isEmpty() || item == null || item.kind() != CellKind.FLUID) {
            return null;
        }
        return new FluidKey(FluidResource.of(fluid));
    }

    /**
     * Lists {@code resource} in filter slot {@code slot}, as a click with it would.
     */
    public void setFilterSlot(final int slot, final NexusResource resource) {
        ClientPacketDistributor.sendToServer(new CellFilterPayload(getMenu().containerId, slot, resource));
    }

    private static String grouped(final long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
