package com.morphengine.nexus.analysis;

import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.menu.GeneratorView;
import com.morphengine.nexus.menu.TankView;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * What a generator does: whether it makes energy and how much, how long the fuel in it lasts, what its input slot and
 * tanks hold, and what its buffer holds.
 */
public final class GeneratorAnalysis {

    private static final int TICKS_PER_SECOND = 20;

    private GeneratorAnalysis() {
    }

    public static void describe(final GeneratorBlockEntity block, final AnalyserLines lines) {
        final GeneratorView view = block.view();
        lines.heading("generator");
        lines.addTranslated("status", "generator." + view.status().name().toLowerCase(Locale.ROOT));
        lines.addTranslated("production", "per_tick", AnalyserLines.perTick(view.production() * TICKS_PER_SECOND));
        lines.addTranslated("capacity_output", "per_tick",
                AnalyserLines.perTick(block.kind().outputPerTick() * TICKS_PER_SECOND));
        if (view.burnTicksTotal() > 0) {
            lines.addTranslated("burning", "seconds_of", seconds(view.burnTicksLeft()),
                    seconds(view.burnTicksTotal()));
        }
        final ItemStack fuel = block.input().getItem(0);
        if (!fuel.isEmpty()) {
            lines.line(Component.translatable("gui.nexus.analyser.input"),
                    Component.empty().append(fuel.getHoverName()).append(" x" + fuel.getCount()));
        }
        for (TankView tank : view.tanks()) {
            final Component fluid = tank.fluid() != null ? tank.fluid().name()
                    : Component.translatable("gui.nexus.analyser.empty");
            lines.add("tank", Component.empty().append(fluid).append(String.format(Locale.ROOT, "  %,d / %,d mB",
                    tank.amount(), tank.capacity())));
        }
        CommonLines.addBuffer(lines, view.stored(), view.capacity());
        CommonLines.addUpgrades(lines, block.upgrades());
    }

    private static String seconds(final int ticks) {
        return String.format(Locale.ROOT, "%.1f", (double) ticks / TICKS_PER_SECOND);
    }
}
