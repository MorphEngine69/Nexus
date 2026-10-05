package com.morphengine.nexus.menu;

import com.morphengine.nexus.machine.MachineActivity;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What the panel of a machine shows. Energy in FE.
 *
 * @param speedPercent how fast the machine works, in percent of the time of a recipe
 * @param activity     what the machine did in its last tick
 * @param progress     how far the work of each line is, in percent, a number for each line
 * @param network      the network the machine draws from; {@code null} when no Nexus is connected
 * @param tank         the tank of a machine that gives a fluid; {@code null} for one that gives items
 */
public record MachineView(
        long stored, long capacity, int speedPercent, MachineActivity activity, List<Integer> progress,
        @Nullable NetworkBadge network, @Nullable TankView tank) {

    public static final MachineView EMPTY = new MachineView(0, 0, 0, MachineActivity.IDLE, List.of(), null, null);

    public MachineView {
        progress = List.copyOf(progress);
    }

    /**
     * @return how far the work of line {@code line} is, in percent; zero for a line the view does not know
     */
    public int progressOf(final int line) {
        return line >= 0 && line < progress.size() ? progress.get(line) : 0;
    }
}
