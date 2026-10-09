package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Pushes energy from a block's buffer into the six blocks around it that
 * accept FE, up to a limit per side. The neighbours' handlers are looked up
 * once and kept current by NeoForge. Server thread only.
 */
final class NeighbourEnergyOutputs {

    private static final Direction[] SIDES = Direction.values();

    private final long maxPerSide;
    private List<BlockCapabilityCache<IEnergyStorage, Direction>> outputs = List.of();

    /**
     * @param maxPerSide FE offered to each side per push, positive
     */
    NeighbourEnergyOutputs(final long maxPerSide) {
        this.maxPerSide = maxPerSide;
    }

    /**
     * @param openSides the sides to push into, one bit for each, at the place of the ordinal of the direction
     * @return whether any energy left the buffer
     */
    boolean push(final ServerLevel level, final BlockPos pos, final EnergyBuffer buffer, final int openSides) {
        if (buffer.stored() == 0) {
            return false;
        }
        boolean pushed = false;
        final List<BlockCapabilityCache<IEnergyStorage, Direction>> neighbours = outputs(level, pos);
        for (int i = 0; i < neighbours.size(); i++) {
            final IEnergyStorage target = (openSides >> i & 1) != 0 ? neighbours.get(i).getCapability() : null;
            if (target != null) {
                pushed |= pushTo(target, buffer);
            }
        }
        return pushed;
    }

    private boolean pushTo(final IEnergyStorage target, final EnergyBuffer buffer) {
        final int offered = (int) buffer.extract(maxPerSide, Action.SIMULATE);
        if (offered == 0) {
            return false;
        }
        final int accepted = target.receiveEnergy(offered, false);
        if (accepted > 0) {
            buffer.extract(accepted, Action.EXECUTE);
        }
        return accepted > 0;
    }

    private List<BlockCapabilityCache<IEnergyStorage, Direction>> outputs(final ServerLevel level, final BlockPos pos) {
        if (outputs.isEmpty()) {
            final List<BlockCapabilityCache<IEnergyStorage, Direction>> created = new ArrayList<>(SIDES.length);
            for (Direction side : SIDES) {
                created.add(BlockCapabilityCache.create(
                        Capabilities.EnergyStorage.BLOCK, level, pos.relative(side), side.getOpposite()));
            }
            outputs = List.copyOf(created);
        }
        return outputs;
    }
}
