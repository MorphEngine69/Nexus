package com.morphengine.nexus.client.screen;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Energy amounts as shown in panels: {@link #amount exactly}, digits grouped, so
 * 100000 reads as 100,000, or {@link #compact short}, in the units of the
 * player's language.
 */
final class EnergyFormat {

    private static final long GROUPED_LIMIT = 10_000;
    private static final double STEP = 1000;
    private static final int TICKS_PER_SECOND = 20;
    private static final double SHORT_DECIMAL_LIMIT = 10;
    private static final double ROUNDING = 100;
    private static final String[] UNIT_KEYS = {"k", "m", "g", "t", "p", "e"};

    private EnergyFormat() {
    }

    /**
     * A figure given per second as FE per tick, short enough for a column: one decimal below ten, whole numbers
     * above, and the short form of {@link #compact} from {@value #GROUPED_LIMIT}; a dash for nothing.
     */
    static String perTick(final long perSecond) {
        if (perSecond <= 0) {
            return "-";
        }
        final double perTick = (double) perSecond / TICKS_PER_SECOND;
        if (perTick < SHORT_DECIMAL_LIMIT) {
            return String.format(Locale.ROOT, "%.1f", perTick);
        }
        return compact(Math.round(perTick)).getString();
    }

    /**
     * A figure given per second as FE per tick with its sign, to two decimals; negative when the device takes more
     * than it gives.
     */
    static String signedPerTick(final long perSecond) {
        return String.format(Locale.ROOT, "%+,.2f", (double) perSecond / TICKS_PER_SECOND);
    }

    /**
     * A figure given per second as FE per tick, to two decimals, for a tooltip.
     */
    static String exactPerTick(final long perSecond) {
        return String.format(Locale.ROOT, "%,.2f", (double) perSecond / TICKS_PER_SECOND);
    }

    static String amount(final long rf) {
        return String.format(Locale.ROOT, "%,d", rf);
    }

    /**
     * The line of the energy stored out of the capacity, with its exact figures
     * for a tooltip if either is written short.
     */
    static StatLine.Stat stored(final long stored, final long capacity) {
        final Component text = Component.translatable("gui.nexus.stats.energy",
                Component.empty().append(compact(stored)).append(" / ").append(compact(capacity)));
        if (Math.max(stored, capacity) < GROUPED_LIMIT) {
            return StatLine.Stat.plain(text);
        }
        return new StatLine.Stat(text, Component.translatable("gui.nexus.energy.exact.stored",
                amount(stored) + " / " + amount(capacity)));
    }

    /**
     * The line of a rate of energy per tick, with its exact figure for a tooltip
     * if it is written short.
     *
     * @param kind {@code input} or {@code output}
     */
    static StatLine.Stat rate(final String kind, final long perTick) {
        final Component text = Component.translatable("gui.nexus.stats." + kind, compact(perTick));
        if (perTick < GROUPED_LIMIT) {
            return StatLine.Stat.plain(text);
        }
        return new StatLine.Stat(text, Component.translatable("gui.nexus.energy.exact." + kind, amount(perTick)));
    }

    /**
     * An amount that stays short however large it grows: grouped digits below
     * {@value #GROUPED_LIMIT}, above it two decimals and a unit, so 18,430,000,000
     * reads as 18.43G in English and as 18.43 млрд in Russian.
     */
    private static Component compact(final long rf) {
        if (rf < GROUPED_LIMIT) {
            return Component.literal(amount(rf));
        }
        double scaled = rf;
        int unit = 0;
        while (Math.round(scaled * ROUNDING) / ROUNDING >= STEP && unit < UNIT_KEYS.length) {
            scaled /= STEP;
            unit++;
        }
        return Component.translatable("gui.nexus.energy.compact", String.format(Locale.ROOT, "%.2f", scaled),
                Component.translatable("gui.nexus.energy.unit." + UNIT_KEYS[unit - 1]));
    }
}
