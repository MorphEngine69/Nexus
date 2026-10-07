package com.morphengine.nexus.generator;

/**
 * Placeholder balance of the generators, to be settled with the rest of the numbers: what each makes a tick of burning,
 * how long a piece or a portion of its fuel burns, how much its tanks hold and how bright it shines.
 */
final class GeneratorBalance {

    /**
     * Ticks of fuel a generator burns each game tick with no Speed Upgrade: a piece of fuel is gone sooner than in a
     * furnace and gives the same FE, at this many times the FE a tick.
     */
    static final int BURN_RATE = 8;

    /** FE for each tick of burning, which is what a piece of fuel gives per tick of the time a furnace burns it. */
    static final long COAL_ENERGY_PER_TICK = 10;
    static final long NETHER_STAR_ENERGY_PER_TICK = 250;
    static final long LAVA_ENERGY_PER_TICK = 20;
    static final long STEAM_ENERGY_PER_TICK = 40;
    static final long BIOFUEL_ENERGY_PER_TICK = 60;

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
