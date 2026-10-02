package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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
    private List<BlockCapabilityCache<EnergyHandler, Direction>> outputs = List.of();

    /**
     * @param maxPerSide FE offered to each side per push, positive
     */
    NeighbourEnergyOutputs(final long maxPerSide) {
        this.maxPerSide = maxPerSide;
    }

    /**
     * @return whether any energy left the buffer
     */
    boolean push(final ServerLevel level, final BlockPos pos, final EnergyBuffer buffer) {
        if (buffer.stored() == 0) {
            return false;
        }
        boolean pushed = false;
        final List<BlockCapabilityCache<EnergyHandler, Direction>> neighbours = outputs(level, pos);
        for (int i = 0; i < neighbours.size(); i++) {
            final EnergyHandler target = neighbours.get(i).getCapability();
            if (target != null) {
                pushed |= pushTo(target, buffer);
            }
        }
        return pushed;
    }

    private boolean pushTo(final EnergyHandler target, final EnergyBuffer buffer) {
        final int offered = (int) buffer.extract(maxPerSide, Action.SIMULATE);
        if (offered == 0) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            final int accepted = target.insert(offered, transaction);
            if (accepted > 0) {
                transaction.commit();
                buffer.extract(accepted, Action.EXECUTE);
            }
            return accepted > 0;
        }
    }

    private List<BlockCapabilityCache<EnergyHandler, Direction>> outputs(final ServerLevel level, final BlockPos pos) {
        if (outputs.isEmpty()) {
            final List<BlockCapabilityCache<EnergyHandler, Direction>> created = new ArrayList<>(SIDES.length);
            for (Direction side : SIDES) {
                created.add(BlockCapabilityCache.create(
                        Capabilities.Energy.BLOCK, level, pos.relative(side), side.getOpposite()));
            }
            outputs = List.copyOf(created);
        }
        return outputs;
    }
}
