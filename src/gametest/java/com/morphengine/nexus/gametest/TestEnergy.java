package com.morphengine.nexus.gametest;

import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The energy of a block under test, as the tests reach it. An Energy Cell and a Nexus let only blocks of a network
 * reach their energy through the capability, so a test goes to the handler of the block entity, as the network does.
 */
final class TestEnergy {

    private TestEnergy() {
    }

    /**
     * @return the handler of the cell or Nexus at {@code pos}, or the energy capability of the block there, asked
     *         from the west
     */
    static EnergyHandler handler(final GameTestHelper helper, final BlockPos pos) {
        final BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        final EnergyHandler handler = EnergyHandler.of(switch (blockEntity) {
            case EnergyCellBlockEntity cell -> cell.energyHandler();
            case NexusBlockEntity nexus -> nexus.energyHandler();
            case null, default -> helper.getLevel()
                    .getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.WEST);
        });
        if (handler == null) {
            throw new GameTestAssertException("no energy handler");
        }
        return handler;
    }

    /**
     * Puts {@code amount} FE into the block at {@code pos} in as many goes as it takes, for a test whose devices
     * spend FE on what they do.
     */
    static void fill(final GameTestHelper helper, final BlockPos pos, final int amount) {
        final EnergyHandler handler = handler(helper, pos);
        int left = amount;
        while (left > 0) {
            try (Transaction transaction = Transaction.openRoot()) {
                final int accepted = handler.insert(left, transaction);
                transaction.commit();
                if (accepted <= 0) {
                    return;
                }
                left -= accepted;
            }
        }
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
