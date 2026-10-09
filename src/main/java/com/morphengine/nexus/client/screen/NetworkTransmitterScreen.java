package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.block.entity.TransmitterStatus;
import com.morphengine.nexus.item.NetworkCardItem;
import com.morphengine.nexus.menu.NetworkTransmitterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Network Transmitter panel, tinted with its network's color: the network,
 * the card slot with where the link goes and whether it goes through, the
 * slot for a Chunk Loader Upgrade, and the inventory.
 */
public final class NetworkTransmitterScreen extends PanelScreen<NetworkTransmitterMenu> {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = NetworkTransmitterMenu.INVENTORY_TOP + 84;
    private static final int TEXT_LEFT = NetworkTransmitterMenu.CARD_SLOT_X + PanelStyle.SLOT_SIZE + 6;
    private static final int LINE_HEIGHT = 11;
    private static final int LABEL_GAP = 11;
    private static final int TEXT_RIGHT_GAP = 8;
    private static final int LINKED_RGB = 0xFF7FD08A;
    private static final int UNREACHABLE_RGB = 0xFFE05A4E;

    public NetworkTransmitterScreen(final NetworkTransmitterMenu menu, final Inventory inventory,
                                    final Component title) {
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
        final int y = topPos + NetworkTransmitterMenu.CARD_SLOT_Y;
        final TransmitterStatus status = getMenu().status();
        graphics.drawString(font, Component.translatable("gui.nexus.transmitter." + status.getSerializedName()), x, y,
                colorOf(status), false);
        final GlobalPos receiver = NetworkCardItem.receiverOf(getMenu().card());
        if (receiver != null) {
            final BlockPos pos = receiver.pos();
            graphics.drawString(font, Component.translatable("gui.nexus.transmitter.receiver", pos.getX(), pos.getY(),
                    pos.getZ()), x, y + LINE_HEIGHT, PanelStyle.TEXT_DIM, false);
        }
        ChunkLoaderNote.draw(graphics, font, x, topPos + NetworkTransmitterMenu.UPGRADE_SLOT_Y,
                imageWidth - TEXT_LEFT - TEXT_RIGHT_GAP, getMenu().pos(), getMenu().holdsChunkLoader());
        graphics.drawString(font, playerInventoryTitle, leftPos + NetworkTransmitterMenu.INVENTORY_LEFT,
                topPos + NetworkTransmitterMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    private static int colorOf(final TransmitterStatus status) {
        return switch (status) {
            case LINKED -> LINKED_RGB;
            case UNREACHABLE -> UNREACHABLE_RGB;
            case NO_CARD -> PanelStyle.TEXT_LIGHT;
        };
    }
}
