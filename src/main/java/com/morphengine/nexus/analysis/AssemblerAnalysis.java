package com.morphengine.nexus.analysis;

import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.assembler.AssemblerRates;
import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * What an Assembler does: how many crafts it is running and how far they are, how many runs it hands out and how
 * often, which comes to runs a second with its upgrades, and which Blueprints it holds.
 */
public final class AssemblerAnalysis {

    private static final int TICKS_PER_SECOND = 20;
    private static final int PERCENT = 100;
    private static final int MAX_LISTED_TASKS = 4;

    private AssemblerAnalysis() {
    }

    public static void describe(final AssemblerBlockEntity block, final AnalyserLines lines) {
        final List<TaskStatus> tasks = block.taskStatuses();
        final int interval = AssemblerRates.rateOf(block.upgrades()).intervalTicks();
        final int runs = AssemblerRates.runsPerOperation(block.upgrades());
        final double operationsPerSecond = (double) TICKS_PER_SECOND / interval;
        lines.heading("assembler");
        lines.addTranslated("status", "status." + (block.isNetworkPowered() ? "working" : "no_power"));
        lines.add("blueprints", String.valueOf(block.blueprints().size()));
        lines.add("priority", String.valueOf(block.blueprintPriority()));
        lines.add("tasks", String.valueOf(tasks.size()));
        for (int index = 0; index < Math.min(MAX_LISTED_TASKS, tasks.size()); index++) {
            final TaskStatus task = tasks.get(index);
            lines.add("crafting", Component.empty().append(NexusResources.of(task.target().resource()).name())
                    .append(String.format(Locale.ROOT, " x%d  %d%%", task.target().amount(),
                            Math.round(task.progress() * PERCENT))));
        }
        lines.addTranslated("interval", "ticks", interval);
        lines.add("runs_per_operation", String.valueOf(runs));
        lines.addTranslated("runs_per_second", "per_second",
                String.format(Locale.ROOT, "%.2f", operationsPerSecond * runs));
        CommonLines.addUpgrades(lines, block.upgrades());
    }
}
