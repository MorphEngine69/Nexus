package com.morphengine.nexus.gametest;

import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The energy of a block under test, as the tests reach it. An Energy Cell lets only blocks of a network reach its
 * energy through the capability, so a test goes to the handler of its block entity, as the pool of a network does.
 */
final class TestEnergy {

    private TestEnergy() {
    }

    /**
     * @return the handler of the cell at {@code pos}, or the energy capability of the block there, asked from the west
     */
    static EnergyHandler handler(final GameTestHelper helper, final BlockPos pos) {
        final BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        final EnergyHandler handler = blockEntity instanceof EnergyCellBlockEntity cell
                ? cell.energyHandler()
                : helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), Direction.WEST);
        if (handler == null) {
            throw helper.assertionException(pos, Component.literal("no energy handler"));
        }
        return handler;
    }

    /**
     * Puts {@code amount} FE into the block at {@code pos}, as much as it takes in one go.
     */
    static void charge(final GameTestHelper helper, final BlockPos pos, final int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            handler(helper, pos).insert(amount, transaction);
            transaction.commit();
        }
    }
}
