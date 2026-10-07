package com.morphengine.nexus.config;

import com.morphengine.nexus.energy.EnergyScale;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The settings of a world: how much energy generators make, machines use and operations cost, what a carried terminal
 * holds and costs to open, how far a Nexus Link reaches, and how many chunks a network may keep loaded. The server
 * config, kept with the world and sent to the players who join it. Before a world is loaded, every getter gives the
 * default, so that a tooltip in the main menu works.
 */
public final class NexusConfig {

    public static final ModConfigSpec SPEC;

    public static final double DEFAULT_MULTIPLIER = 1.0;
    public static final int DEFAULT_TERMINAL_CAPACITY = 100_000;
    public static final int DEFAULT_TERMINAL_OPEN_COST = 2_000;
    /** Blocks a Nexus Link reaches with no Range Upgrade, then with one, two and three. */
    public static final int[] DEFAULT_LINK_RANGES = {64, 128, 256, 512};
    /** No limit on the chunks a network keeps loaded. */
    public static final int UNLIMITED_CHUNKS = 0;

    private static final String TRANSLATION_PREFIX = "nexus.configuration.";
    private static final double MIN_MULTIPLIER = 0.01;
    private static final double MAX_MULTIPLIER = 100.0;
    private static final int MAX_BLOCKS = 1_000_000;
    private static final int MAX_CHUNKS = 10_000;

    private static final ModConfigSpec.DoubleValue GENERATION;
    private static final ModConfigSpec.DoubleValue MACHINE_USE;
    private static final ModConfigSpec.DoubleValue OPERATION_PRICE;
    private static final ModConfigSpec.BooleanValue TERMINAL_DISCHARGES;
    private static final ModConfigSpec.IntValue TERMINAL_OPEN_COST;
    private static final ModConfigSpec.IntValue TERMINAL_CAPACITY;
    private static final ModConfigSpec.IntValue[] LINK_RANGES = new ModConfigSpec.IntValue[DEFAULT_LINK_RANGES.length];
    private static final ModConfigSpec.IntValue CHUNK_LIMIT;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("energy");
        GENERATION = multiplier(builder, "generation_multiplier");
        MACHINE_USE = multiplier(builder, "machine_use_multiplier");
        OPERATION_PRICE = multiplier(builder, "operation_price_multiplier");
        builder.pop();
        builder.push("terminal");
        TERMINAL_DISCHARGES = builder.translation(TRANSLATION_PREFIX + "terminal_discharges")
                .define("terminal_discharges", true);
        TERMINAL_OPEN_COST = builder.translation(TRANSLATION_PREFIX + "terminal_open_cost")
                .defineInRange("terminal_open_cost", DEFAULT_TERMINAL_OPEN_COST, 0, Integer.MAX_VALUE);
        TERMINAL_CAPACITY = builder.translation(TRANSLATION_PREFIX + "terminal_capacity")
                .defineInRange("terminal_capacity", DEFAULT_TERMINAL_CAPACITY, 1, Integer.MAX_VALUE);
        builder.pop();
        builder.push("link");
        for (int upgrades = 0; upgrades < LINK_RANGES.length; upgrades++) {
            LINK_RANGES[upgrades] = builder.translation(TRANSLATION_PREFIX + "link_range_" + upgrades)
                    .defineInRange("link_range_" + upgrades, DEFAULT_LINK_RANGES[upgrades], 1, MAX_BLOCKS);
        }
        builder.pop();
        builder.push("chunks");
        CHUNK_LIMIT = builder.translation(TRANSLATION_PREFIX + "chunk_limit")
                .defineInRange("chunk_limit", UNLIMITED_CHUNKS, 0, MAX_CHUNKS);
        builder.pop();
        SPEC = builder.build();
    }

    private NexusConfig() {
    }

    private static ModConfigSpec.DoubleValue multiplier(final ModConfigSpec.Builder builder, final String name) {
        return builder.translation(TRANSLATION_PREFIX + name)
                .defineInRange(name, DEFAULT_MULTIPLIER, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }

    /**
     * @return the share of the FE generators make, in percent; a hundred changes nothing
     */
    public static int generationPercent() {
        return EnergyScale.percentOf(read(GENERATION));
    }

    /**
     * @return the share of the FE the work of machines costs, in percent; a hundred changes nothing
     */
    public static int machineUsePercent() {
        return EnergyScale.percentOf(read(MACHINE_USE));
    }

    /**
     * @return the share of what operations of devices cost, in percent; a hundred changes nothing
     */
    public static int operationPricePercent() {
        return EnergyScale.percentOf(read(OPERATION_PRICE));
    }

    /**
     * @return whether a carried Nexus Terminal holds a charge that opening it uses
     */
    public static boolean terminalDischarges() {
        return read(TERMINAL_DISCHARGES);
    }

    /**
     * @return FE that opening a carried Nexus Terminal uses
     */
    public static int terminalOpenCost() {
        return read(TERMINAL_OPEN_COST);
    }

    /**
     * @return FE a carried Nexus Terminal holds
     */
    public static int terminalCapacity() {
        return read(TERMINAL_CAPACITY);
    }

    /**
     * @param upgrades Range Upgrades in the Link; more than the Link takes count as the most
     * @return blocks the Link reaches
     */
    public static int linkRange(final int upgrades) {
        return read(LINK_RANGES[Math.clamp(upgrades, 0, LINK_RANGES.length - 1)]);
    }

    /**
     * @return the most chunks one network keeps loaded; {@value #UNLIMITED_CHUNKS} for no limit
     */
    public static int chunkLimit() {
        return read(CHUNK_LIMIT);
    }

    private static <T> T read(final ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
