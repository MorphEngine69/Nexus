package com.morphengine.nexus.resource;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * How amounts of one kind of resource are shown and stepped through: items are
 * counted, fluids are stored in millibuckets and shown in buckets, energy is
 * counted in FE.
 */
public enum AmountUnit {

    ITEMS(1, "", "gui.nexus.quantity.items", 1),
    MILLIBUCKETS(1000, "B", "gui.nexus.quantity.millibuckets", 1000),
    /** Placeholder step until the numbers are settled. */
    ENERGY(1, "", "gui.nexus.quantity.energy", 10_000);

    private static final long THOUSAND = 1000;
    private static final String[] MAGNITUDES = {"", "K", "M", "G", "T", "P", "E"};
    private static final double ONE_DECIMAL_BELOW = 10;

    private final long unitsPerWhole;
    private final String wholeSuffix;
    private final String quantityKey;
    private final long step;

    AmountUnit(final long unitsPerWhole, final String wholeSuffix, final String quantityKey, final long step) {
        this.unitsPerWhole = unitsPerWhole;
        this.wholeSuffix = wholeSuffix;
        this.quantityKey = quantityKey;
        this.step = step;
    }

    /**
     * @return units in one whole of this unit: one item, a bucket of 1000
     *         millibuckets, one FE
     */
    public long unitsPerWhole() {
        return unitsPerWhole;
    }

    /**
     * @return units in one step of the resource: what a device moves in one
     *         operation before upgrades, what a Pusher keeps stocked by default,
     *         and what one turn of the mouse wheel changes that by. One item, a
     *         bucket, 10,000 FE.
     */
    public long step() {
        return step;
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
     * @return the exact amount with grouped digits and its unit, such as {@code 1,000 mB}
     */
    public MutableComponent quantity(final long amount) {
        return Component.translatable(quantityKey, String.format(Locale.ROOT, "%,d", amount));
    }

    /**
     * @return the exact amount as held, for a tooltip, such as {@code Stored: 1,000 mB}
     */
    public MutableComponent exact(final long amount) {
        return Component.translatable("gui.nexus.amount.stored", quantity(amount));
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
