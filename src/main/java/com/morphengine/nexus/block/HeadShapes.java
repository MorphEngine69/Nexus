package com.morphengine.nexus.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * The shapes of a device that has a head on one face and a cable arm to every network block on its other sides, such as
 * a Puller or an External Vault.
 */
final class HeadShapes {

    private static final int SIDES = Direction.values().length;
    private static final int MASKS = 1 << SIDES;
    private static final VoxelShape[] SHAPES = buildShapes();

    private HeadShapes() {
    }

    /**
     * @param facing       the face that holds the head
     * @param attachedMask the sides an arm reaches to, one bit for each, at the place of the ordinal of the direction
     * @return the head with the core behind it and an arm of cable profile for each attached side
     */
    static VoxelShape of(final Direction facing, final int attachedMask) {
        return SHAPES[facing.ordinal() * MASKS + attachedMask];
    }

    private static VoxelShape[] buildShapes() {
        final Map<Direction, VoxelShape> heads = Shapes.rotateAll(Shapes.or(
                Block.box(3, 3, 0, 13, 13, 5), Block.box(5, 5, 5, 11, 11, 11)));
        final Map<Direction, VoxelShape> arms = Shapes.rotateAll(Block.box(5, 5, 0, 11, 11, 5));
        final VoxelShape[] shapes = new VoxelShape[SIDES * MASKS];
        for (Direction facing : Direction.values()) {
            for (int mask = 0; mask < MASKS; mask++) {
                shapes[facing.ordinal() * MASKS + mask] = withArms(heads.get(facing), arms, mask);
            }
        }
        return shapes;
    }

    private static VoxelShape withArms(final VoxelShape head, final Map<Direction, VoxelShape> arms, final int mask) {
        VoxelShape shape = head;
        for (Direction side : Direction.values()) {
            if ((mask & 1 << side.ordinal()) != 0) {
                shape = Shapes.or(shape, arms.get(side));
            }
        }
        return shape;
    }
}
