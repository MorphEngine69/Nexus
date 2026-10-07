package com.morphengine.nexus.energy;

/**
 * A kind of operation that takes FE from the network, with what it costs before the size of the network and Speed
 * Upgrades make it dearer. Placeholder balance until the numbers are settled.
 */
public enum OperationKind {

    /** A player takes resources out of the network at a terminal. */
    TERMINAL_TAKE(5),
    /** A Puller or Pusher moves resources. */
    TRANSFER(10),
    /** A Placer or Remover works on the world. */
    WORLD(40),
    /** An Assembler sends one run of a recipe. */
    ASSEMBLER_RUN(100);

    private final long baseCost;

    OperationKind(final long baseCost) {
        this.baseCost = baseCost;
    }

    /**
     * @return FE the operation costs in a small network, without Speed Upgrades
     */
    public long baseCost() {
        return baseCost;
    }
}
