package com.morphengine.nexus.analysis;

import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Finds what is worth telling about a block of the mod, by what kind of block it is, and has it added to the lines of
 * an Analyser. A block with nothing of its own to tell adds nothing; its energy figures are added for every device
 * anyway. Server thread only.
 */
public final class Analysers {

    private Analysers() {
    }

    public static void describe(final BlockEntity target, final AnalyserLines lines) {
        switch (target) {
            case TransferDeviceBlockEntity device -> TransferAnalysis.describe(device, lines);
            case MachineBlockEntity machine -> MachineAnalysis.describe(machine, lines);
            case GeneratorBlockEntity generator -> GeneratorAnalysis.describe(generator, lines);
            case AssemblerBlockEntity assembler -> AssemblerAnalysis.describe(assembler, lines);
            case EnergyCellBlockEntity cell -> CellAnalysis.describe(cell, lines);
            case NexusLinkBlockEntity link -> LinkAnalysis.describe(link, lines);
            default -> { }
        }
    }
}
