package com.morphengine.nexus.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The text a panel with a slot for a Chunk Loader Upgrade writes beside it, centred on the slot. It names the upgrade
 * and says what it does: which chunk it keeps loaded, or what it would do, for the upgrade is optional and without it
 * a device works, the chunk being then loaded only while the game loads it anyway. The text takes at most
 * {@value #MAX_LINES} lines of the width it is given; where the full one needs more, a shorter one is written.
 */
final class ChunkLoaderNote {

    private static final int MAX_LINES = 2;
    private static final int HELD_RGB = 0xFF7FD08A;

    private ChunkLoaderNote() {
    }

    /**
     * @param y        where the slot is, the top of the slot itself
     * @param pos      where the device stands
     * @param held     whether the device holds a Chunk Loader Upgrade
     * @param maxWidth width in pixels the line may take before it wraps
     */
    static void draw(final GuiGraphics graphics, final Font font, final int x, final int y,
                     final int maxWidth, final BlockPos pos, final boolean held) {
        final int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        final int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        final List<Component> options = held
                ? List.of(Component.translatable("gui.nexus.chunk_loader.on", chunkX, chunkZ),
                        Component.translatable("gui.nexus.chunk_loader.on.short", chunkX, chunkZ))
                : List.of(Component.translatable("gui.nexus.chunk_loader.off"),
                        Component.translatable("gui.nexus.chunk_loader.off.short"));
        Component text = options.getLast();
        for (Component option : options) {
            if (font.split(option, maxWidth).size() <= MAX_LINES) {
                text = option;
                break;
            }
        }
        final int height = font.split(text, maxWidth).size() * font.lineHeight;
        final int top = y - 1 + (PanelStyle.SLOT_SIZE - height) / 2;
        graphics.drawWordWrap(font, text, x, top, maxWidth, held ? HELD_RGB : PanelStyle.TEXT_DIM);
    }
}
