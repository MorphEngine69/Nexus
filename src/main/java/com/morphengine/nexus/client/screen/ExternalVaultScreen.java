package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.client.input.MouseButtonEvent;
import com.morphengine.nexus.menu.ExternalVaultMenu;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.networking.FilterModePayload;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.storage.ExternalAccess;
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
 * External Vault panel, tinted with the color of its network: the network, the priority of the vault with buttons to
 * change it, the filter of the resources the network may use, what the network may do with them, and the inventory. The
 * filter slots work as {@link FilterGrid} describes, for items and for fluids; a recipe viewer can drop both on them.
 */
public final class ExternalVaultScreen extends PanelScreen<ExternalVaultMenu> implements FilterScreen {

    private static final int IMAGE_WIDTH = 224;
    private static final int IMAGE_HEIGHT = ExternalVaultMenu.INVENTORY_TOP + 84;
    private static final int PRIORITY_TOP = 32;
    private static final int FILTER_LABEL_TOP = ExternalVaultMenu.FILTER_TOP - 12;
    private static final int MODE_WIDTH = 72;
    private static final int BUTTON_HEIGHT = 14;
    private static final int ACCESS_TOP = 50;
    private static final int LABEL_GAP = 11;

    private PriorityRow priority;
    private FilterGrid<ExternalVaultMenu> filterGrid;

    public ExternalVaultScreen(final ExternalVaultMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.priority = createPriorityRow();
        this.filterGrid = createFilterGrid();
    }

    @Override
    protected void init() {
        super.init();
        priority = createPriorityRow();
        filterGrid = createFilterGrid();
    }

    private PriorityRow createPriorityRow() {
        return new PriorityRow(panelBounds(), topPos + PRIORITY_TOP, ExternalVaultMenu.BUTTON_PRIORITY);
    }

    private FilterGrid<ExternalVaultMenu> createFilterGrid() {
        return new FilterGrid<>(getMenu(), leftPos + ExternalVaultMenu.FILTER_LEFT,
                topPos + ExternalVaultMenu.FILTER_TOP, ExternalVaultMenu.FILTER_COLUMNS);
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().badge());
    }

    private PanelBounds modeButton() {
        return new PanelBounds(leftPos + ExternalVaultMenu.FILTER_LEFT
                + ExternalVaultMenu.FILTER_COLUMNS * PanelStyle.SLOT_SIZE - MODE_WIDTH,
                topPos + FILTER_LABEL_TOP - (BUTTON_HEIGHT - font.lineHeight) / 2 - 1, MODE_WIDTH, BUTTON_HEIGHT);
    }

    private PanelBounds accessButton() {
        return new PanelBounds(leftPos + PanelStyle.PADDING, topPos + ACCESS_TOP,
                IMAGE_WIDTH - 2 * PanelStyle.PADDING, BUTTON_HEIGHT);
    }

    @Override
    protected void extractPanel(
            final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final NetworkBadge network = getMenu().badge();
        PanelStyle.drawNetwork(graphics, font, network, leftPos, topPos);
        priority.draw(graphics, font, style, getMenu().priority());
        graphics.drawString(font, Component.translatable("gui.nexus.filter"), leftPos + PanelStyle.PADDING,
                topPos + FILTER_LABEL_TOP, PanelStyle.TEXT_DIM, false);
        final String modeKey = getMenu().filter().mode() == FilterMode.ALLOW
                ? "gui.nexus.filter.whitelist" : "gui.nexus.filter.blacklist";
        style.drawButton(graphics, font, modeButton(), Component.translatable(modeKey));
        filterGrid.draw(graphics, style, mouseX, mouseY);
        style.drawButton(graphics, font, accessButton(), accessLabel(getMenu().settings().access()));
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        graphics.drawString(font, playerInventoryTitle, leftPos + ExternalVaultMenu.INVENTORY_LEFT,
                topPos + ExternalVaultMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    private static Component accessLabel(final ExternalAccess access) {
        return Component.translatable("gui.nexus.external.access." + access.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (accessButton().contains(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.nexus.external.access."
                    + getMenu().settings().access().name().toLowerCase(Locale.ROOT) + ".hint"), mouseX, mouseY);
            return;
        }
        final NexusResource resource = filterGrid.resourceAt(mouseX, mouseY);
        if (getMenu().getCarried().isEmpty() && resource != null) {
            graphics.renderComponentTooltip(font, ResourceRenderers.tooltip(resource), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        if (minecraft == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (modeButton().contains(event.x(), event.y())) {
            PacketDistributor.sendToServer(new FilterModePayload(getMenu().containerId));
            return true;
        }
        if (accessButton().contains(event.x(), event.y()) && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, ExternalVaultMenu.BUTTON_ACCESS);
            return true;
        }
        return priority.click(minecraft, getMenu().containerId, event.x(), event.y()) || filterGrid.click(event)
                || super.mouseClicked(mouseX, mouseY, button);
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
}
