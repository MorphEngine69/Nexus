package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * An amount written small in the lower right corner of a slot's icon, as the
 * vanilla stack count sits.
 */
final class SlotAmounts {

    private static final int ICON_SIZE = 16;
    private static final int AMOUNT_RGB = 0xFFFFFFFF;
    private static final float AMOUNT_SCALE = 0.5F;
    private static final float ABOVE_ITEMS = 200.0F;

    private SlotAmounts() {
    }

    /**
     * @param iconLeft left edge of the 16 pixel icon the amount belongs to
     */
    static void draw(
            final GuiGraphics graphics, final Font font, final String amount, final int iconLeft,
            final int iconTop) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, ABOVE_ITEMS);
        graphics.pose().scale(AMOUNT_SCALE, AMOUNT_SCALE, 1.0F);
        final int textRight = (int) ((iconLeft + ICON_SIZE) / AMOUNT_SCALE);
        final int textBottom = (int) ((iconTop + ICON_SIZE) / AMOUNT_SCALE);
        graphics.drawString(font, amount, textRight - font.width(amount), textBottom - font.lineHeight + 1,
                AMOUNT_RGB, true);
        graphics.pose().popPose();
    }
}
