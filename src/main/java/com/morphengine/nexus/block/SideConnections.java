package com.morphengine.nexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.function.BiPredicate;

/**
 * One boolean block state property per side telling the model whether something
 * is attached there: a cable arm, or a port on the Nexus.
 */
public final class SideConnections {

    private SideConnections() {
    }

    public static void addProperties(final StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction side : Direction.values()) {
            builder.add(property(side));
        }
    }

    public static BlockState detached(final BlockState state) {
        BlockState result = state;
        for (Direction side : Direction.values()) {
            result = result.setValue(property(side), false);
        }
        return result;
    }

    /**
     * @param attaches tells, for a side and the block beyond it, whether something attaches there
     */
    public static BlockState attachedTo(
            final BlockState state, final BlockGetter level, final BlockPos pos, final BiPredicate<Direction,
            BlockState> attaches) {
        BlockState result = state;
        for (Direction side : Direction.values()) {
            result = result.setValue(property(side), attaches.test(side, level.getBlockState(pos.relative(side))));
        }
        return result;
    }

    public static BlockState withSide(final BlockState state, final Direction side, final boolean attached) {
        return state.setValue(property(side), attached);
    }

    public static boolean isAttached(final BlockState state, final Direction side) {
        return state.getValue(property(side));
    }

    /**
     * @return one bit per side with something attached, bit {@code side.ordinal()}
     */
    public static int attachedMask(final BlockState state) {
        int mask = 0;
        for (Direction side : Direction.values()) {
            if (state.getValue(property(side))) {
                mask |= 1 << side.ordinal();
            }
        }
        return mask;
    }

    private static BooleanProperty property(final Direction side) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(side);
    }
}
