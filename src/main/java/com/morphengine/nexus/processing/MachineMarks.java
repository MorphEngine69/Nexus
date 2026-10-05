package com.morphengine.nexus.processing;

import com.morphengine.nexus.machine.MachineTier;

import java.util.ArrayList;
import java.util.List;

/**
 * The marks of the tier on the lower bar of a machine: as many squares as its rank, in a bone of the model for each
 * rank, of which a machine shows the one of its own rank.
 */
public final class MachineMarks {

    private MachineMarks() {
    }

    public static String boneOf(final int rank) {
        return "marks_" + rank;
    }

    /**
     * @return the names of the bones of the marks of every rank but {@code rank}
     */
    public static List<String> bonesOfOtherRanks(final int rank) {
        final List<String> names = new ArrayList<>();
        for (int other = 1; other <= MachineTier.QUANTUM.rank(); other++) {
            if (other != rank) {
                names.add(boneOf(other));
            }
        }
        return List.copyOf(names);
    }
}
