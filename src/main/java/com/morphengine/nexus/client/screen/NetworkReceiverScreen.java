package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.menu.NetworkReceiverMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Network Receiver panel, tinted with its network's color: the network, the
 * slot for a Chunk Loader Upgrade with the chunk it keeps loaded, and the
 * inventory.
 */
public final class NetworkReceiverScreen extends PanelScreen<NetworkReceiverMenu> {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = NetworkReceiverMenu.INVENTORY_TOP + 84;
    private static final int TEXT_LEFT = NetworkReceiverMenu.UPGRADE_SLOT_X + PanelStyle.SLOT_SIZE + 6;
    private static final int TEXT_RIGHT_GAP = 8;
    private static final int LABEL_GAP = 11;

    public NetworkReceiverScreen(final NetworkReceiverMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().badge());
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        PanelStyle.drawNetwork(graphics, font, getMenu().badge(), leftPos, topPos);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        ChunkLoaderNote.draw(graphics, font, leftPos + TEXT_LEFT, topPos + NetworkReceiverMenu.UPGRADE_SLOT_Y,
                imageWidth - TEXT_LEFT - TEXT_RIGHT_GAP, getMenu().pos(), getMenu().holdsChunkLoader());
        graphics.text(font, playerInventoryTitle, leftPos + NetworkReceiverMenu.INVENTORY_LEFT,
                topPos + NetworkReceiverMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }
}
