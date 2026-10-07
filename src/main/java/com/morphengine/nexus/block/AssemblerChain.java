package com.morphengine.nexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Assemblers chained to one machine: an Assembler whose face touches another
 * Assembler works with the machine at the end of the chain, so several of
 * them, each with its own Blueprints, share one machine the way a single one
 * could only touch from one side. The last Assembler of the chain, the one
 * facing the machine, is its root. Chains are read from block states, so the
 * server and the client see the same one; they end at {@value #MAX_LENGTH}
 * Assemblers or at an unloaded block.
 */
public final class AssemblerChain {

    static final int MAX_LENGTH = 8;

    private AssemblerChain() {
    }

    /**
     * @param pos an Assembler
     * @return the root of the chain {@code pos} is in and the face of the root;
     *         the Assembler itself when it faces no other Assembler, or when
     *         its chain loops or runs too long to reach a machine
     */
    public static Link linkOf(final LevelReader level, final BlockPos pos) {
        final BlockState start = level.getBlockState(pos);
        if (!(start.getBlock() instanceof AssemblerBlock)) {
            throw new IllegalArgumentException("no Assembler at " + pos + ": " + start);
        }
        BlockPos current = pos;
        Direction face = start.getValue(AssemblerBlock.FACING);
        for (int length = 1; length <= MAX_LENGTH; length++) {
            final BlockPos next = current.relative(face);
            if (!level.hasChunkAt(next)) {
                return new Link(current, face);
            }
            final BlockState faced = level.getBlockState(next);
            if (!(faced.getBlock() instanceof AssemblerBlock)) {
                return new Link(current, face);
            }
            current = next;
            face = faced.getValue(AssemblerBlock.FACING);
        }
        return new Link(pos, start.getValue(AssemblerBlock.FACING));
    }

    /**
     * @param root the root of a chain
     * @return every Assembler whose chain ends at {@code root}, the root first
     */
    public static List<BlockPos> membersOf(final LevelReader level, final BlockPos root) {
        final List<BlockPos> members = new ArrayList<>();
        members.add(root);
        final Deque<BlockPos> open = new ArrayDeque<>();
        open.add(root);
        while (!open.isEmpty() && members.size() < MAX_LENGTH * Direction.values().length) {
            final BlockPos reached = open.poll();
            for (Direction side : Direction.values()) {
                final BlockPos feeder = reached.relative(side);
                if (facesInto(level, feeder, side.getOpposite()) && !members.contains(feeder)) {
                    members.add(feeder);
                    open.add(feeder);
                }
            }
        }
        return List.copyOf(members);
    }

    private static boolean facesInto(final LevelReader level, final BlockPos pos, final Direction face) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        final BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof AssemblerBlock && state.getValue(AssemblerBlock.FACING) == face;
    }

    /**
     * Where a chain meets its machine.
     *
     * @param root the Assembler facing the machine
     * @param face the root's face
     */
    public record Link(BlockPos root, Direction face) {

        public BlockPos machine() {
            return root.relative(face);
        }
    }
}
