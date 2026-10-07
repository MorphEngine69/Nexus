package com.morphengine.nexus.analysis;

import com.morphengine.nexus.api.network.DeviceEnergyUse;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The lines every block shares: what it does with energy, which upgrades it holds, how full its buffer is.
 */
public final class CommonLines {

    private CommonLines() {
    }

    /**
     * Adds the energy figures of a device, in FE per tick, averaged over the last ten seconds.
     */
    public static void addEnergy(final AnalyserLines lines, final DeviceEnergyUse use) {
        lines.heading("energy");
        lines.addTranslated("drawn", "per_tick", AnalyserLines.perTick(use.drawn()));
        lines.addTranslated("supplied", "per_tick", AnalyserLines.perTick(use.supplied()));
        lines.addTranslated("tolls", "per_tick", AnalyserLines.perTick(use.tolls()));
        lines.addTranslated("net", "per_tick", AnalyserLines.signedPerTick(use.supplied() - use.spent()));
    }

    /**
     * Adds the upgrades in {@code upgrades}, each kind with how many of it, or that there are none.
     */
    public static void addUpgrades(final AnalyserLines lines, final Container upgrades) {
        lines.heading("upgrades");
        final Map<Item, Integer> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < upgrades.getContainerSize(); slot++) {
            final ItemStack stack = upgrades.getItem(slot);
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            lines.addTranslated("upgrades_held", "none");
            return;
        }
        for (Map.Entry<Item, Integer> held : counts.entrySet()) {
            lines.line(held.getKey().getDefaultInstance().getHoverName(),
                    Component.literal("x" + held.getValue()));
        }
    }

    /**
     * Adds how full a buffer is, in FE.
     */
    public static void addBuffer(final AnalyserLines lines, final long stored, final long capacity) {
        lines.addTranslated("buffer", "stored_of", String.format(Locale.ROOT, "%,d", stored),
                String.format(Locale.ROOT, "%,d", capacity));
    }
}
