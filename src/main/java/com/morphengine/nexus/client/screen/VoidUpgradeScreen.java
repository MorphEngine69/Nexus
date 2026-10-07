package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.menu.VoidUpgradeMenu;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Panel of the Void Upgrade in hand: the nine items and fluids it has the network destroy. The slots work as
 * {@link FilterGrid} describes, and a recipe viewer can drop items and fluids on them.
 */
public final class VoidUpgradeScreen extends PanelScreen<VoidUpgradeMenu> implements FilterScreen {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = VoidUpgradeMenu.INVENTORY_TOP + 84;
    private static final int HINT_TOP = PanelStyle.NETWORK_TOP;
    private static final int FILTER_LABEL_TOP = 46;
    private static final int LABEL_GAP = 11;

    private FilterGrid<VoidUpgradeMenu> filterGrid;

    public VoidUpgradeScreen(final VoidUpgradeMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.filterGrid = createFilterGrid();
    }

    @Override
    protected void init() {
        super.init();
        filterGrid = createFilterGrid();
    }

    private FilterGrid<VoidUpgradeMenu> createFilterGrid() {
        return new FilterGrid<>(getMenu(), leftPos + VoidUpgradeMenu.FILTER_LEFT, topPos + VoidUpgradeMenu.FILTER_TOP,
                VoidUpgradeMenu.FILTER_SLOTS);
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.tinted(NetworkColor.DEFAULT.rgb());
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final int x = leftPos + PanelStyle.PADDING;
        graphics.text(font, Component.translatable("gui.nexus.void.hint"), x, topPos + HINT_TOP,
                PanelStyle.TEXT_LIGHT, false);
        graphics.text(font, Component.translatable("gui.nexus.void.list"), x, topPos + FILTER_LABEL_TOP,
                PanelStyle.TEXT_DIM, false);
        filterGrid.draw(graphics, style, mouseX, mouseY);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        graphics.text(font, playerInventoryTitle, leftPos + VoidUpgradeMenu.INVENTORY_LEFT,
                topPos + VoidUpgradeMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        final NexusResource resource = filterGrid.resourceAt(mouseX, mouseY);
        if (getMenu().getCarried().isEmpty() && resource != null) {
            graphics.setComponentTooltipForNextFrame(font, ResourceRenderers.tooltip(resource), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return filterGrid.click(event) || super.mouseClicked(event, doubleClick);
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
