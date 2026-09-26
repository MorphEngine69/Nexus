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

import java.util.ArrayList;
import java.util.List;

/**
 * Storage Vault panel, tinted with its network's color: the network, the
 * vault's priority with buttons to change it, the cell slots with a lamp beside
 * each, and the inventory.
 */
public final class StorageVaultScreen extends PanelScreen<StorageVaultMenu> {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = StorageVaultMenu.INVENTORY_TOP + 84;
    private static final int PADDING = 10;
    private static final int NETWORK_TOP = 22;
    private static final int PRIORITY_TOP = 36;
    private static final int BUTTON_WIDTH = 22;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private static final int LAMP_WIDTH = 2;
    private static final int LAMP_HEIGHT = 4;
    private static final int LABEL_GAP = 11;
    private static final String[] BUTTON_LABELS = {"-10", "-1", "+1", "+10"};
    private static final int[] BUTTON_IDS = {
        StorageVaultMenu.BUTTON_LOWER_TEN, StorageVaultMenu.BUTTON_LOWER,
        StorageVaultMenu.BUTTON_RAISE, StorageVaultMenu.BUTTON_RAISE_TEN,
    };
    private static final int[] LAMP_RGB = {0xFF1E1F26, 0xFF4FB060, 0xFFD9892E, 0xFFD2403A};

    private final List<PanelBounds> buttons = new ArrayList<>();

    public StorageVaultScreen(final StorageVaultMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        final int firstLeft = leftPos + IMAGE_WIDTH - PADDING - BUTTON_LABELS.length * (BUTTON_WIDTH + BUTTON_GAP)
                + BUTTON_GAP;
        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            buttons.add(new PanelBounds(firstLeft + i * (BUTTON_WIDTH + BUTTON_GAP), topPos + PRIORITY_TOP,
                    BUTTON_WIDTH, BUTTON_HEIGHT));
        }
    }

    @Override
    protected PanelStyle style() {
        final NetworkBadge network = getMenu().badge();
        return network != null ? PanelStyle.tinted(network.color().rgb()) : PanelStyle.neutral();
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final NetworkBadge network = getMenu().badge();
        graphics.text(font, network != null ? Component.translatable("gui.nexus.network", network.name())
                : Component.translatable("gui.nexus.no_network"),
                leftPos + PADDING, topPos + NETWORK_TOP, PanelStyle.TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.nexus.vault.priority", getMenu().priority()),
                leftPos + PADDING, topPos + PRIORITY_TOP + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1,
                PanelStyle.TEXT_LIGHT, false);
        for (int i = 0; i < buttons.size(); i++) {
            style.drawButton(graphics, font, buttons.get(i), Component.literal(BUTTON_LABELS[i]));
        }
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
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).contains(event.x(), event.y()) && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, BUTTON_IDS[i]);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
