package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.level.NetworkNeighbours;
import com.morphengine.nexus.transport.SideConfig;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Who may reach the energy of an Energy Cell through which of its six sides. A block of a network always may, from
 * any side, as a Puller, a Pusher or a generator beside the cell does. A block of another mod may only through a side
 * the player has opened, and only as the mode of that side allows: to put FE in, to take it out, or both. The player
 * answers for the energy that leaves through a side they opened; every side starts closed.
 */
final class CellSides {

    private final IEnergyStorage fullAccess;
    private final IEnergyStorage receiveOnly;
    private final IEnergyStorage giveOnly;
    private SideConfig<Direction> config = SideConfig.closed(Direction.class);

    /**
     * @param fullAccess  the handler that puts FE in and takes it out
     * @param receiveOnly the handler that only puts FE in
     * @param giveOnly    the handler that only takes FE out
     */
    CellSides(final IEnergyStorage fullAccess, final IEnergyStorage receiveOnly, final IEnergyStorage giveOnly) {
        this.fullAccess = Objects.requireNonNull(fullAccess, "fullAccess must not be null");
        this.receiveOnly = Objects.requireNonNull(receiveOnly, "receiveOnly must not be null");
        this.giveOnly = Objects.requireNonNull(giveOnly, "giveOnly must not be null");
    }

    SideMode mode(final Direction side) {
        return config.mode(side);
    }

    /**
     * @return whether the mode of {@code side} changed
     */
    boolean set(final Direction side, final SideMode mode) {
        final SideConfig<Direction> changed = config.with(side, mode);
        final boolean differs = !changed.equals(config);
        config = changed;
        return differs;
    }

    int bits() {
        return config.toBits();
    }

    void restore(final int bits) {
        config = SideConfig.fromBits(Direction.class, bits);
    }

    /**
     * @param side the side of the cell that is asked about; {@code null} when the asker names no side
     * @return what the cell shows a block that asks from {@code side}: the full handler to a block of a network, the
     *         handler the mode of the side allows to any other, and {@code null} when that side is closed
     */
    @Nullable IEnergyStorage handlerFor(final Level level, final BlockPos pos, final @Nullable Direction side) {
        if (side == null) {
            return null;
        }
        return NetworkNeighbours.hasNetworkBlockBeyond(level, pos, side) ? fullAccess : handlerFor(config.mode(side));
    }

    private @Nullable IEnergyStorage handlerFor(final SideMode mode) {
        return switch (mode) {
            case CLOSED -> null;
            case INPUT -> receiveOnly;
            case OUTPUT -> giveOnly;
            case BOTH -> fullAccess;
        };
    }
}
