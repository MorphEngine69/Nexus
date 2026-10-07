package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * An amount written small in the lower right corner of a slot's icon, as the
 * vanilla stack count sits.
 */
final class SlotAmounts {

    private static final int ICON_SIZE = 16;
    private static final int AMOUNT_RGB = 0xFFFFFFFF;
    private static final float AMOUNT_SCALE = 0.5F;

    private SlotAmounts() {
    }

    /**
     * @param iconLeft left edge of the 16 pixel icon the amount belongs to
     */
    static void draw(
            final GuiGraphicsExtractor graphics, final Font font, final String amount, final int iconLeft,
            final int iconTop) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(AMOUNT_SCALE, AMOUNT_SCALE);
        final int textRight = (int) ((iconLeft + ICON_SIZE) / AMOUNT_SCALE);
        final int textBottom = (int) ((iconTop + ICON_SIZE) / AMOUNT_SCALE);
        graphics.text(font, amount, textRight - font.width(amount), textBottom - font.lineHeight + 1,
                AMOUNT_RGB, true);
        graphics.pose().popMatrix();
    }
}
