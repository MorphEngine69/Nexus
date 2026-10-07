package com.morphengine.nexus.analysis;

import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;

/**
 * What a Nexus Link does: how far terminals reach it, whether those in other dimensions do, and whether it keeps its
 * chunk loaded.
 */
public final class LinkAnalysis {

    private LinkAnalysis() {
    }

    public static void describe(final NexusLinkBlockEntity block, final AnalyserLines lines) {
        lines.heading("link");
        lines.addTranslated("range", "blocks", block.range());
        lines.addTranslated("other_dimensions", block.reachesOtherDimensions() ? "yes" : "no");
        lines.addTranslated("chunk_loader", UpgradeLimits.count(block.upgrades(), UpgradeTypes.CHUNK_LOADER.get()) > 0
                ? "yes" : "no");
        CommonLines.addUpgrades(lines, block.upgrades());
    }
}
