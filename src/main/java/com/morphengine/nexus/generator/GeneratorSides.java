package com.morphengine.nexus.generator;

import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.machine.MachineSides;
import com.morphengine.nexus.processing.MachineFacing;
import com.morphengine.nexus.transport.SideConfig;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The settings of the sides of a generator, counted from its front: where fuel goes in (items and fluids) and where the
 * FE comes out. Every side is open at first, as the generator took and gave through any side before they could be
 * chosen. Server thread only.
 */
public final class GeneratorSides {

    private SideConfig<MachineSide> config = MachineSides.allOpen();
    private int openOutputSides;
    private @Nullable Direction maskFacing;

    public SideConfig<MachineSide> config() {
        return config;
    }

    /**
     * @return whether the side changed
     */
    public boolean set(final MachineSide side, final SideMode mode) {
        if (config.mode(side) == mode) {
            return false;
        }
        config = config.with(side, mode);
        maskFacing = null;
        return true;
    }

    /**
     * @param bits what {@link SideConfig#toBits()} gave; below zero for a generator saved before it had sides, which
     *             keeps every side open
     */
    public void restore(final int bits) {
        config = bits < 0 ? MachineSides.allOpen() : SideConfig.fromBits(MachineSide.class, bits);
        maskFacing = null;
    }

    /**
     * @param state     the state of the generator, which says where its front faces
     * @param worldSide a side of the generator in the world
     */
    public SideMode modeOn(final BlockState state, final Direction worldSide) {
        return config.mode(MachineFacing.sideOf(state.getValue(HorizontalDirectionalBlock.FACING), worldSide));
    }

    /**
     * @return a bit for each side of the world that gives FE, at the place of the ordinal of its direction; kept until
     *         the facing or the sides change
     */
    public int outputMask(final BlockState state) {
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (facing != maskFacing) {
            int open = 0;
            for (MachineSide side : MachineSide.values()) {
                if (config.mode(side).allowsOutput()) {
                    open |= 1 << MachineFacing.worldSide(facing, side).ordinal();
                }
            }
            openOutputSides = open;
            maskFacing = facing;
        }
        return openOutputSides;
    }
}
