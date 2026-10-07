package com.morphengine.nexus.integration.top;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.NetworkDeviceBlock;
import com.morphengine.nexus.probe.BlockProbes;
import com.morphengine.nexus.probe.ProbeLine;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.IProgressStyle;
import mcjty.theoneprobe.api.NumberFormat;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * Draws the report of a block of Nexus in the box of The One Probe: its text lines, and its bars of energy, tanks and
 * progress. Called on the server, where the block entity is. The text of a bar is written here whole, so that the
 * numbers read alike in every language, and every bar is as wide as the longest of them needs.
 */
final class ProbeInfoProvider implements IProbeInfoProvider {

    private static final Identifier ID = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "probe");
    private static final int ENERGY_FILLED = 0xFFDD0000;
    private static final int ENERGY_ALTERNATE = 0xFF430000;
    private static final int PROGRESS_FILLED = 0xFF44AA44;
    private static final int PROGRESS_ALTERNATE = 0xFF2E7A2E;
    private static final int PROGRESS_FULL = 100;
    private static final int THOUSAND = 1000;
    private static final String UNITS = "kMGTPE";
    /** Pixels a character of a bar takes, twice over, and a little more than the font needs for the longer words
     * of other languages. */
    private static final int HALF_PIXELS_PER_CHARACTER = 11;
    private static final int BAR_PADDING = 8;
    private static final int MIN_BAR_WIDTH = 100;

    @Override
    public Identifier getID() {
        return ID;
    }

    @Override
    public void addProbeInfo(
            final ProbeMode mode, final IProbeInfo info, final Player player, final Level level,
            final BlockState state, final IProbeHitData data) {
        if (!(state.getBlock() instanceof NetworkDeviceBlock)
                || !(level.getBlockEntity(data.getPos()) instanceof BlockEntity block)) {
            return;
        }
        final List<ProbeLine> lines = BlockProbes.of(block).lines();
        final int width = barWidth(lines);
        for (ProbeLine line : lines) {
            switch (line) {
                case ProbeLine.Text text -> info.mcText(text.text());
                case ProbeLine.Energy energy -> info.progress(energy.stored(), energy.capacity(),
                        bar(info, width, energyText(energy)).filledColor(ENERGY_FILLED)
                                .alternateFilledColor(ENERGY_ALTERNATE));
                case ProbeLine.Tank tank -> tank(info, width, tank);
                case ProbeLine.Progress progress -> info.progress(progress.percent(), PROGRESS_FULL,
                        bar(info, width, progressText(progress)).filledColor(PROGRESS_FILLED)
                                .alternateFilledColor(PROGRESS_ALTERNATE));
            }
        }
    }

    private static void tank(final IProbeInfo info, final int width, final ProbeLine.Tank tank) {
        if (tank.contents().isEmpty()) {
            info.progress(0, tank.capacity(), bar(info, width, tankText(tank)));
        } else {
            info.tankSimple((int) tank.capacity(), tank.contents(), bar(info, width, tankText(tank)));
        }
    }

    /**
     * A bar that shows {@code text} as it is: the number format of the probe writes nothing of its own.
     */
    private static IProgressStyle bar(final IProbeInfo info, final int width, final String text) {
        return info.defaultProgressStyle().numberFormat(NumberFormat.NONE).suffix(text).width(width);
    }

    private static int barWidth(final List<ProbeLine> lines) {
        int longest = 0;
        for (ProbeLine line : lines) {
            longest = Math.max(longest, switch (line) {
                case ProbeLine.Energy energy -> energyText(energy).length();
                case ProbeLine.Tank tank -> tankText(tank).length();
                case ProbeLine.Progress progress -> progressText(progress).length();
                case ProbeLine.Text text -> 0;
            });
        }
        return Math.max(MIN_BAR_WIDTH, longest * HALF_PIXELS_PER_CHARACTER / 2 + BAR_PADDING);
    }

    private static String energyText(final ProbeLine.Energy energy) {
        return compact(energy.stored()) + " / " + compact(energy.capacity()) + " FE";
    }

    private static String tankText(final ProbeLine.Tank tank) {
        return tank.name().getString() + ": " + compact(tank.contents().getAmount()) + " / "
                + compact(tank.capacity()) + " mB";
    }

    private static String progressText(final ProbeLine.Progress progress) {
        return progress.label().getString() + ": " + progress.percent() + "%";
    }

    /**
     * A number in a few characters: whole below a thousand, then in thousands, millions and so on, with one decimal
     * where it is not a round number, as in 199.9k and 200k.
     */
    private static String compact(final long value) {
        if (value < THOUSAND) {
            return Long.toString(value);
        }
        final int unit = (int) (Math.log(value) / Math.log(THOUSAND));
        final String scaled = String.format(Locale.ROOT, "%.1f", value / Math.pow(THOUSAND, unit));
        return (scaled.endsWith(".0") ? scaled.substring(0, scaled.length() - 2) : scaled) + UNITS.charAt(unit - 1);
    }
}
