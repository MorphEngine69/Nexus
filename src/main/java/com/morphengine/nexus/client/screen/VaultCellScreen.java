package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.storage.CellUsage;
import com.morphengine.nexus.client.input.MouseButtonEvent;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.menu.VaultCellMenu;
import com.morphengine.nexus.networking.FilterModePayload;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Panel of the Vault Cell in hand: how full it is, its filter and its name.
 * The filter slots work as {@link FilterGrid} describes; for a fluid cell an
 * item stands for the fluid inside. A recipe viewer can drop items and fluids
 * on the filter slots too.
 */
public final class VaultCellScreen extends PanelScreen<VaultCellMenu> implements FilterScreen {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = VaultCellMenu.INVENTORY_TOP + 84;
    private static final int USAGE_TOP = PanelStyle.NETWORK_TOP;
    private static final int LINE_HEIGHT = 11;
    private static final int FILTER_LABEL_TOP = 46;
    private static final int MODE_WIDTH = 72;
    private static final int MODE_HEIGHT = 14;
    private static final int LABEL_GAP = 11;

    private FilterGrid<VaultCellMenu> filterGrid;

    public VaultCellScreen(final VaultCellMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.filterGrid = createFilterGrid();
    }

    @Override
    protected void init() {
        super.init();
        filterGrid = createFilterGrid();
    }

    private FilterGrid<VaultCellMenu> createFilterGrid() {
        return new FilterGrid<>(getMenu(), leftPos + VaultCellMenu.FILTER_LEFT, topPos + VaultCellMenu.FILTER_TOP,
                VaultCellMenu.FILTER_SLOTS);
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.tinted(NetworkColor.DEFAULT.rgb());
    }

    private PanelBounds modeButton() {
        return new PanelBounds(leftPos + IMAGE_WIDTH - PanelStyle.PADDING - MODE_WIDTH,
                topPos + FILTER_LABEL_TOP - (MODE_HEIGHT - font.lineHeight) / 2 - 1, MODE_WIDTH, MODE_HEIGHT);
    }

    @Override
    protected void extractPanel(
            final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final VaultCellItem item = getMenu().cellItem();
        if (item == null) {
            return;
        }
        final CellUsage usage = item.openStorage(getMenu().cell()).usage();
        final int x = leftPos + PanelStyle.PADDING;
        graphics.drawString(font, Component.translatable("tooltip.nexus.cell.bytes", grouped(usage.usedBytes()),
                grouped(usage.totalBytes())), x, topPos + USAGE_TOP, PanelStyle.TEXT_LIGHT, false);
        graphics.drawString(font, Component.translatable("tooltip.nexus.cell.types", usage.storedTypes(),
                usage.maxTypes()), x, topPos + USAGE_TOP + LINE_HEIGHT, PanelStyle.TEXT_LIGHT, false);
        graphics.drawString(font, Component.translatable("gui.nexus.filter"), x, topPos + FILTER_LABEL_TOP,
                PanelStyle.TEXT_DIM, false);
        final String modeKey = getMenu().filter().mode() == FilterMode.ALLOW
                ? "gui.nexus.filter.whitelist" : "gui.nexus.filter.blacklist";
        style.drawButton(graphics, font, modeButton(), Component.translatable(modeKey));
        filterGrid.draw(graphics, style, mouseX, mouseY);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        graphics.drawString(font, playerInventoryTitle, leftPos + VaultCellMenu.INVENTORY_LEFT,
                topPos + VaultCellMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        final NexusResource resource = filterGrid.resourceAt(mouseX, mouseY);
        if (getMenu().getCarried().isEmpty() && resource != null) {
            graphics.renderComponentTooltip(font, ResourceRenderers.tooltip(resource), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        if (modeButton().contains(event.x(), event.y())) {
            PacketDistributor.sendToServer(new FilterModePayload(getMenu().containerId));
            return true;
        }
        return filterGrid.click(event) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public List<Rect2i> filterSlotAreas() {
        return filterGrid.areas();
    }

    @Override
    public @Nullable NexusResource filterEntryOf(final ItemStack stack) {
        return getMenu().cellItem() != null ? filterGrid.entryOf(stack) : null;
    }

    @Override
    public @Nullable NexusResource filterEntryOf(final FluidStack fluid) {
        return getMenu().cellItem() != null ? filterGrid.entryOf(fluid) : null;
    }

    @Override
    public void setFilterSlot(final int slot, final NexusResource resource) {
        filterGrid.send(slot, resource);
    }

    private static String grouped(final long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
