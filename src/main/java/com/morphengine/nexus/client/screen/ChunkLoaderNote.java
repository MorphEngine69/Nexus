package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;

/**
 * The line a panel with a slot for a Chunk Loader Upgrade writes beside it:
 * which chunk it keeps loaded, or that it keeps none.
 */
final class ChunkLoaderNote {

    private static final int HELD_RGB = 0xFF7FD08A;

    private ChunkLoaderNote() {
    }

    /**
     * @param pos      where the device stands
     * @param held     whether the device holds a Chunk Loader Upgrade
     * @param maxWidth width in pixels the line may take before it wraps
     */
    static void draw(final GuiGraphicsExtractor graphics, final Font font, final int x, final int y,
                     final int maxWidth, final BlockPos pos, final boolean held) {
        if (held) {
            graphics.textWithWordWrap(font, Component.translatable("gui.nexus.chunk_loader.on",
                    SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())),
                    x, y, maxWidth, HELD_RGB);
        } else {
            graphics.textWithWordWrap(font, Component.translatable("gui.nexus.chunk_loader.off"), x, y, maxWidth,
                    PanelStyle.TEXT_DIM);
        }
    }
}
