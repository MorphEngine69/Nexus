package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.NexusLinkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Nexus Link panel, tinted with its network's color: the network, the slots
 * for Range, Dimension and Chunk Loader Upgrades with how far the link reaches
 * and whether it reaches other dimensions, and the inventory.
 */
public final class NexusLinkScreen extends PanelScreen<NexusLinkMenu> {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = NexusLinkMenu.INVENTORY_TOP + 84;
    private static final int TEXT_LEFT = NexusLinkMenu.UPGRADE_SLOT_X + PanelStyle.SLOT_SIZE + 6;
    private static final int LABEL_GAP = 11;
    private static final int NOTE_GAP = 8;

    public NexusLinkScreen(final NexusLinkMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().badge());
    }

    @Override
    protected void extractPanel(
            final GuiGraphics graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        PanelStyle.drawNetwork(graphics, font, getMenu().badge(), leftPos, topPos);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        final int x = leftPos + TEXT_LEFT;
        final int y = topPos + NexusLinkMenu.UPGRADE_SLOT_Y;
        graphics.drawString(font, Component.translatable("gui.nexus.link.range", getMenu().range()), x,
                y - 1 + (PanelStyle.SLOT_SIZE - font.lineHeight) / 2, PanelStyle.TEXT_LIGHT, false);
        drawBesideSlot(graphics, Component.translatable(getMenu().holdsDimension() ? "gui.nexus.link.dimension.on"
                : "gui.nexus.link.dimension.off"), y + NexusLinkMenu.UPGRADE_ROW_HEIGHT);
        ChunkLoaderNote.draw(graphics, font, x, y + 2 * NexusLinkMenu.UPGRADE_ROW_HEIGHT,
                imageWidth - TEXT_LEFT - NOTE_GAP, getMenu().pos(), getMenu().holdsChunkLoader());
        graphics.drawString(font, playerInventoryTitle, leftPos + NexusLinkMenu.INVENTORY_LEFT,
                topPos + NexusLinkMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    private void drawBesideSlot(final GuiGraphics graphics, final Component text, final int slotTop) {
        final int width = imageWidth - TEXT_LEFT - NOTE_GAP;
        final int height = font.split(text, width).size() * font.lineHeight;
        graphics.drawWordWrap(font, text, leftPos + TEXT_LEFT, slotTop - 1 + (PanelStyle.SLOT_SIZE - height) / 2,
                width, PanelStyle.TEXT_LIGHT);
    }
}
