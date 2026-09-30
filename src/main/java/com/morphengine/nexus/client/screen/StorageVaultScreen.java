package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.block.VaultLamp;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.menu.StorageVaultMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Storage Vault panel, tinted with its network's color: the network, the
 * vault's priority with buttons to change it, the cell slots with a lamp beside
 * each, and the inventory.
 */
public final class StorageVaultScreen extends PanelScreen<StorageVaultMenu> {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = StorageVaultMenu.INVENTORY_TOP + 84;
    private static final int PRIORITY_TOP = 32;
    private static final int LAMP_WIDTH = 2;
    private static final int LAMP_HEIGHT = 4;
    private static final int LABEL_GAP = 11;
    private static final int[] LAMP_RGB = {0xFF1E1F26, 0xFF4FB060, 0xFFD9892E, 0xFFD2403A};

    private PriorityRow priority;

    public StorageVaultScreen(final StorageVaultMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.priority = createPriorityRow();
    }

    @Override
    protected void init() {
        super.init();
        priority = createPriorityRow();
    }

    private PriorityRow createPriorityRow() {
        return new PriorityRow(panelBounds(), topPos + PRIORITY_TOP, StorageVaultMenu.BUTTON_PRIORITY);
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().badge());
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final NetworkBadge network = getMenu().badge();
        PanelStyle.drawNetwork(graphics, font, network, leftPos, topPos);
        priority.draw(graphics, font, style, getMenu().priority());
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        drawLamps(graphics);
        graphics.text(font, playerInventoryTitle, leftPos + StorageVaultMenu.INVENTORY_LEFT,
                topPos + StorageVaultMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    /**
     * A lamp at the right edge of each cell slot, as on the vault's front.
     */
    private void drawLamps(final GuiGraphicsExtractor graphics) {
        final StorageVaultBlockEntity vault = getMenu().blockEntity();
        if (vault == null) {
            return;
        }
        for (int slot = 0; slot < StorageVaultBlockEntity.SLOTS; slot++) {
            final VaultLamp lamp = vault.lampAt(slot);
            final Slot cell = getMenu().slots.get(slot);
            final int x = leftPos + cell.x + PanelStyle.SLOT_SIZE - 2 - LAMP_WIDTH - 1;
            final int y = topPos + cell.y;
            graphics.fill(x, y, x + LAMP_WIDTH, y + LAMP_HEIGHT, LAMP_RGB[lamp.ordinal()]);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return minecraft != null && priority.click(minecraft, getMenu().containerId, event.x(), event.y())
                || super.mouseClicked(event, doubleClick);
    }
}
