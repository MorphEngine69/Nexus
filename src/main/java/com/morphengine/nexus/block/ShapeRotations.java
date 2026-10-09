package com.morphengine.nexus.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a shape drawn for a block that faces north to every direction.
 */
final class ShapeRotations {

    private static final int MAX_X = 3;
    private static final int MAX_Y = 4;
    private static final int MAX_Z = 5;

    private ShapeRotations() {
    }

    /**
     * @param north a shape inside the block, drawn facing north
     * @return the shape facing each of the six directions
     */
    static Map<Direction, VoxelShape> rotateAll(final VoxelShape north) {
        final List<double[]> boxes = north.toAabbs().stream()
                .map(box -> new double[] {box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ}).toList();
        final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            VoxelShape turned = Shapes.empty();
            for (double[] box : boxes) {
                turned = Shapes.or(turned, turn(box, facing));
            }
            shapes.put(facing, turned.optimize());
        }
        return shapes;
    }

    private static VoxelShape turn(final double[] box, final Direction facing) {
        final double[] first = move(box[0], box[1], box[2], facing);
        final double[] second = move(box[MAX_X], box[MAX_Y], box[MAX_Z], facing);
        return Shapes.box(Math.min(first[0], second[0]), Math.min(first[1], second[1]), Math.min(first[2], second[2]),
                Math.max(first[0], second[0]), Math.max(first[1], second[1]), Math.max(first[2], second[2]));
    }

    private static double[] move(final double x, final double y, final double z, final Direction facing) {
        return switch (facing) {
            case SOUTH -> new double[] {1 - x, y, 1 - z};
            case WEST -> new double[] {z, y, 1 - x};
            case EAST -> new double[] {1 - z, y, x};
            case UP -> new double[] {x, 1 - z, y};
            case DOWN -> new double[] {x, z, 1 - y};
            default -> new double[] {x, y, z};
        };
    }
}
