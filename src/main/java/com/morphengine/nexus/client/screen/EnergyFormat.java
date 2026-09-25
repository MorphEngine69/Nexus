package com.morphengine.nexus.client.screen;

import java.util.Locale;

/**
 * Energy amounts as shown in panels: digits grouped, so 100000 reads as 100,000.
 */
final class EnergyFormat {

    private EnergyFormat() {
    }

    static String amount(final long rf) {
        return String.format(Locale.ROOT, "%,d", rf);
    }
}
