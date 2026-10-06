package com.morphengine.nexus.metal;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Objects;

/**
 * How a block of a metal is broken and how it sounds. Such a block always asks for the right tool to drop anything.
 *
 * @param hardness        time to break, positive
 * @param blastResistance resistance to explosions, not negative
 */
public record BlockFeel(float hardness, float blastResistance, SoundType sound, MapColor mapColor) {

    private static final float DEEPSLATE_HARDNESS_FACTOR = 1.5F;

    public BlockFeel {
        Objects.requireNonNull(sound, "sound");
        Objects.requireNonNull(mapColor, "mapColor");
        if (hardness <= 0 || blastResistance < 0) {
            throw new IllegalArgumentException(
                    "block feel needs positive hardness and a resistance: " + hardness + ", " + blastResistance);
        }
    }

    /**
     * @return the same block cut out of deepslate: one and a half times harder, as deepslate ores are
     */
    public BlockFeel inDeepslate() {
        return new BlockFeel(hardness * DEEPSLATE_HARDNESS_FACTOR, blastResistance, SoundType.DEEPSLATE,
                MapColor.DEEPSLATE);
    }

    public BlockBehaviour.Properties apply(final BlockBehaviour.Properties properties) {
        return properties.mapColor(mapColor).strength(hardness, blastResistance).sound(sound)
                .requiresCorrectToolForDrops();
    }
}
