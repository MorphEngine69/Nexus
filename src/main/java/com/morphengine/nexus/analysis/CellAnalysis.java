package com.morphengine.nexus.analysis;

import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What an Energy Cell does: where it stands in the pool of its network, how much it passes in a tick at most, how full
 * it is, and what it charges.
 */
public final class CellAnalysis {

    private CellAnalysis() {
    }

    public static void describe(final EnergyCellBlockEntity block, final AnalyserLines lines) {
        lines.heading("cell");
        lines.add("priority", String.valueOf(block.energyPriority()));
        if (block.getBlockState().getBlock() instanceof EnergyCellBlock cell) {
            lines.addTranslated("throughput_limit", "per_tick", String.valueOf(cell.tier().maxTransfer()));
        }
        final ItemStack charging = block.chargingSlot().getItem(0);
        if (!charging.isEmpty()) {
            lines.line(Component.translatable("gui.nexus.analyser.charging"), charging.getHoverName());
        }
        CommonLines.addBuffer(lines, block.energyBuffer().stored(), block.energyBuffer().capacity());
        CommonLines.addUpgrades(lines, block.upgrades());
    }
}
