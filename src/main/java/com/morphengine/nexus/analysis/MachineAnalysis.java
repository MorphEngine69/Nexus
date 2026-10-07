package com.morphengine.nexus.analysis;

import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.machine.Machine;
import com.morphengine.nexus.machine.MachineActivity;
import com.morphengine.nexus.machine.MachineLine;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * What a machine does: how fast it works, which of its lines are working on what and how far, how much FE that costs
 * and how much it makes in a second, and what its buffer and tank hold.
 */
public final class MachineAnalysis {

    private static final int TICKS_PER_SECOND = 20;
    private static final int PERCENT = 100;
    /** The most lines of work listed, so that a machine of the top tier does not fill the panel. */
    private static final int MAX_LISTED_LINES = 7;

    private MachineAnalysis() {
    }

    public static void describe(final MachineBlockEntity block, final AnalyserLines lines) {
        final Machine machine = block.machine();
        lines.heading("machine");
        lines.addTranslated("status", "activity." + machine.activity().name().toLowerCase(Locale.ROOT));
        lines.addTranslated("speed", "percent", machine.speedPercent());
        lines.addTranslated("lines_working", "of", workingLines(machine), machine.lineCount());
        listWork(machine, lines);
        final TankView tank = block.view().tank();
        if (tank != null) {
            addTank(lines, tank);
        }
        CommonLines.addBuffer(lines, machine.energy().stored(), machine.energy().capacity());
        CommonLines.addUpgrades(lines, block.upgrades());
    }

    private static int workingLines(final Machine machine) {
        int working = 0;
        for (int index = 0; index < machine.lineCount(); index++) {
            if (machine.line(index).isRunning()) {
                working++;
            }
        }
        return working;
    }

    private static void listWork(final Machine machine, final AnalyserLines lines) {
        long costPerTick = 0;
        int listed = 0;
        for (int index = 0; index < machine.lineCount(); index++) {
            final MachineLine line = machine.line(index);
            final Optional<MachineRecipe> recipe = line.activeRecipe();
            if (recipe.isEmpty()) {
                continue;
            }
            costPerTick += costOf(machine, recipe.get());
            if (listed++ < MAX_LISTED_LINES) {
                lines.add("making", describeWork(machine, line, recipe.get()));
            }
        }
        lines.addTranslated("work_cost", "per_tick", AnalyserLines.perTick(costPerTick * TICKS_PER_SECOND));
    }

    /**
     * @return FE a tick the line pays: faster work uses proportionally more, an Efficiency Upgrade less
     */
    private static long costOf(final Machine machine, final MachineRecipe recipe) {
        return recipe.energyPerTick() * machine.speedPercent() / PERCENT * machine.costPercent() / PERCENT;
    }

    private static Component describeWork(final Machine machine, final MachineLine line, final MachineRecipe recipe) {
        final ResourceAmount output = recipe.output();
        final double secondsPerRun = (double) recipe.ticks() * PERCENT / machine.speedPercent() / TICKS_PER_SECOND;
        return Component.empty().append(NexusResources.of(output.resource()).name())
                .append(String.format(Locale.ROOT, " x%d  %d%%  (%.2f/s)", output.amount(), line.progressPercent(),
                        output.amount() / secondsPerRun));
    }

    private static void addTank(final AnalyserLines lines, final TankView tank) {
        final Component fluid = tank.fluid() != null ? tank.fluid().name()
                : Component.translatable("gui.nexus.analyser.empty");
        lines.add("tank", Component.empty().append(fluid).append(String.format(Locale.ROOT, "  %,d / %,d mB",
                tank.amount(), tank.capacity())));
    }

    /**
     * @return whether the machine is working this tick
     */
    static boolean isWorking(final Machine machine) {
        return machine.activity() == MachineActivity.WORKING;
    }
}
