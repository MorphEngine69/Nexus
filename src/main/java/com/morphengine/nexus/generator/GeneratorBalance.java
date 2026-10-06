package com.morphengine.nexus.generator;

/**
 * Placeholder balance of the generators, to be settled with the rest of the numbers: what each makes a tick, how long
 * a piece or a portion of its fuel burns, how much its tanks hold and how bright it shines.
 */
final class GeneratorBalance {

    static final long COAL_ENERGY_PER_TICK = 40;
    static final long NETHER_STAR_ENERGY_PER_TICK = 200;
    static final long LAVA_ENERGY_PER_TICK = 60;
    static final long STEAM_ENERGY_PER_TICK = 100;
    static final long BIOFUEL_ENERGY_PER_TICK = 120;

    /** A nether star burns twenty minutes. */
    static final int NETHER_STAR_BURN_TICKS = 24_000;
    /** A bucket of lava burns twenty thousand ticks, as in a furnace. */
    static final int LAVA_BURN_TICKS_PER_MILLIBUCKET = 20;
    static final int STEAM_BURN_TICKS_PER_PORTION = 25;
    static final int STEAM_WATER_PER_PORTION = 2;
    static final int BIOFUEL_BURN_TICKS_PER_MILLIBUCKET = 30;

    static final int TANK_CAPACITY_MILLIBUCKETS = 8_000;

    static final int COAL_LIGHT = 13;
    static final int NETHER_STAR_LIGHT = 12;
    static final int LAVA_LIGHT = 15;
    static final int STEAM_LIGHT = 7;
    static final int BIOFUEL_LIGHT = 9;

    private GeneratorBalance() {
    }
}
