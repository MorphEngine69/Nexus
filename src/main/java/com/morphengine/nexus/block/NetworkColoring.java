package com.morphengine.nexus.block;

import com.morphengine.nexus.api.network.NetworkColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * Shows the color of a network on the models of its devices through
 * {@link NetworkDeviceBlock#NETWORK_COLOR}. The Nexus lets the player pick one of
 * the dye colors; each has its own accent textures, so the block state carries
 * the dye rather than the raw RGB.
 */
public final class NetworkColoring {

    /**
     * Shown by devices no Nexus reaches, and by networks whose color is not one
     * of the dyes, such as {@link NetworkColor#DEFAULT}: the standard Nexus blue.
     */
    public static final DyeColor UNCONNECTED = DyeColor.BLUE;

    private NetworkColoring() {
    }

    public static NetworkColor colorOf(final DyeColor dye) {
        return new NetworkColor(dye.getTextureDiffuseColor() & NetworkColor.MAX_RGB);
    }

    public static DyeColor dyeOf(final NetworkColor color) {
        Objects.requireNonNull(color, "color must not be null");
        for (DyeColor dye : DyeColor.values()) {
            if (colorOf(dye).equals(color)) {
                return dye;
            }
        }
        return UNCONNECTED;
    }

    /**
     * Repaints the device at {@code pos} if it shows another color. Clients get
     * the new state with the usual block update and redraw it; neighbours are
     * not notified, the shape and connections do not change. Server side only.
     */
    public static void paint(final Level level, final BlockPos pos, final DyeColor dye) {
        if (!level.isLoaded(pos)) {
            return;
        }
        final BlockState state = level.getBlockState(pos);
        if (state.hasProperty(NetworkDeviceBlock.NETWORK_COLOR)
                && state.getValue(NetworkDeviceBlock.NETWORK_COLOR) != dye) {
            level.setBlock(pos, state.setValue(NetworkDeviceBlock.NETWORK_COLOR, dye), Block.UPDATE_CLIENTS);
        }
    }
}
