package com.morphengine.nexus.assembler;

import com.morphengine.nexus.transport.TransferRate;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.world.Container;

/**
 * How often an Assembler works and how many runs it hands out each time, given the upgrades it holds.
 */
public final class AssemblerRates {

    /** With a Stack Upgrade a task hands out up to a stack of runs at once instead of one. */
    private static final int STACK_RUNS = 64;

    private AssemblerRates() {
    }

    /**
     * @return how often the Assembler works: sooner with every Speed Upgrade
     */
    public static TransferRate rateOf(final Container upgrades) {
        return TransferRate.of(UpgradeLimits.count(upgrades, UpgradeTypes.SPEED.get()), 0);
    }

    /**
     * @return runs the Assembler's tasks hand out per operation: one, and one more for every Speed Upgrade, all of
     *         that a stack of times over with a Stack Upgrade; a task never hands out more runs than it still has
     */
    public static int runsPerOperation(final Container upgrades) {
        final int perStack = UpgradeLimits.count(upgrades, UpgradeTypes.STACK.get()) > 0 ? STACK_RUNS : 1;
        return (1 + UpgradeLimits.count(upgrades, UpgradeTypes.SPEED.get())) * perStack;
    }
}
