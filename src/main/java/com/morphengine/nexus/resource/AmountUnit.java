package com.morphengine.nexus.resource;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * How amounts of one kind of resource are shown: items are counted, fluids are
 * stored in millibuckets and shown in buckets.
 */
public enum AmountUnit {

    ITEMS(1, "", "gui.nexus.amount.items"),
    MILLIBUCKETS(1000, "B", "gui.nexus.amount.millibuckets");

    private static final long THOUSAND = 1000;
    private static final String[] MAGNITUDES = {"", "K", "M", "G", "T", "P", "E"};
    private static final double ONE_DECIMAL_BELOW = 10;

    private final long unitsPerWhole;
    private final String wholeSuffix;
    private final String exactKey;

    AmountUnit(final long unitsPerWhole, final String wholeSuffix, final String exactKey) {
        this.unitsPerWhole = unitsPerWhole;
        this.wholeSuffix = wholeSuffix;
        this.exactKey = exactKey;
    }

    /**
     * @return units in one whole of this unit: one item, or a bucket of 1000 millibuckets
     */
    public long unitsPerWhole() {
        return unitsPerWhole;
    }

    /**
     * @return at most five characters for the corner of a slot, such as
     *         {@code 64}, {@code 1.2K}, {@code 15B} or {@code 250mB}
     */
    public String compact(final long amount) {
        if (unitsPerWhole > 1 && amount < unitsPerWhole) {
            return amount + "m" + wholeSuffix;
        }
        return compactWhole((double) amount / unitsPerWhole) + wholeSuffix;
    }

    /**
     * @return the exact amount with grouped digits, for a tooltip
     */
    public MutableComponent exact(final long amount) {
        return Component.translatable(exactKey, String.format(Locale.ROOT, "%,d", amount));
    }

    private static String compactWhole(final double value) {
        double scaled = value;
        int magnitude = 0;
        while (scaled >= THOUSAND && magnitude < MAGNITUDES.length - 1) {
            scaled /= THOUSAND;
            magnitude++;
        }
        final double tenths = Math.floor(scaled * ONE_DECIMAL_BELOW) / ONE_DECIMAL_BELOW;
        final boolean showTenths = scaled < ONE_DECIMAL_BELOW && tenths != Math.floor(tenths);
        final String number = showTenths
                ? String.format(Locale.ROOT, "%.1f", tenths)
                : Long.toString((long) scaled);
        return number + MAGNITUDES[magnitude];
    }
}
