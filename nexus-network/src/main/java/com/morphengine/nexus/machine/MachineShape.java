package com.morphengine.nexus.machine;

/**
 * How a kind of machine is built: how many input slots a line has, and how many lines the machine has at most however
 * high its tier. Most machines have one input slot in a line and a line more for every tier; an Alloy Smelter has
 * three inputs in its only line.
 *
 * @param inputsPerLine input slots of a line, positive
 * @param maxLines      most lines, positive; a tier that has more lines is held to this
 */
public record MachineShape(int inputsPerLine, int maxLines) {

    /** The shape of most machines: one input slot to a line, as many lines as the tier has. */
    public static final MachineShape SINGLE_INPUT = new MachineShape(1, Integer.MAX_VALUE);

    public MachineShape {
        if (inputsPerLine <= 0 || maxLines <= 0) {
            throw new IllegalArgumentException(
                    "inputsPerLine and maxLines must be positive: " + inputsPerLine + ", " + maxLines);
        }
    }

    /**
     * @return the lines a machine of {@code tier} has
     */
    public int linesOf(final MachineTier tier) {
        return Math.min(tier.linePairs(), maxLines);
    }
}
