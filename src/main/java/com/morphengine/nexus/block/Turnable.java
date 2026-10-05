package com.morphengine.nexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A device with a front that the Wrench turns. The front is the face the block works on, and it takes no cable; turning
 * moves it, and the ports of the block follow.
 */
public interface Turnable {

    /**
     * The order the Wrench goes through the sides: round the horizontal, then up, then down. A block that cannot
     * face a side is passed over.
     */
    List<Direction> TURN_ORDER = List.of(
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN);

    EnumProperty<Direction> facingProperty();

    /**
     * @param oriented the state with its new front, whose ports are not yet worked out
     * @return {@code oriented} with a port on every side that now takes a cable
     */
    BlockState withPorts(BlockState oriented, BlockGetter level, BlockPos pos);

    /**
     * @return {@code state} with its front on the next side in {@link #TURN_ORDER} that the block can face
     */
    default BlockState turned(final BlockState state) {
        final EnumProperty<Direction> facing = facingProperty();
        final int current = TURN_ORDER.indexOf(state.getValue(facing));
        for (int step = 1; step <= TURN_ORDER.size(); step++) {
            final Direction candidate = TURN_ORDER.get((current + step) % TURN_ORDER.size());
            if (facing.getPossibleValues().contains(candidate)) {
                return state.setValue(facing, candidate);
            }
        }
        return state;
    }

    /**
     * @return {@code state} with its front on {@code side}; {@code null} when the block cannot face that side
     */
    default @Nullable BlockState facing(final BlockState state, final Direction side) {
        final EnumProperty<Direction> facing = facingProperty();
        return facing.getPossibleValues().contains(side) ? state.setValue(facing, side) : null;
    }
}
