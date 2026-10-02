package com.morphengine.nexus.level;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;

/**
 * A network's energy as other mods reach it through its Nexus: inserts fill and
 * extracts drain the pool's buffers in the order the pool ranks them, each
 * through a handler of its own that takes part in the transaction. The network's current buffers are read
 * on every call, so the handler stays valid across rebuilds. Server thread only.
 */
final class NetworkEnergyHandler implements EnergyHandler {

    private final NetworkState network;

    NetworkEnergyHandler(final NetworkState network) {
        this.network = network;
    }

    @Override
    public long getAmountAsLong() {
        return network.energy().stored();
    }

    @Override
    public long getCapacityAsLong() {
        return network.energy().capacity();
    }

    @Override
    public int insert(final int amount, final TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        final List<EnergyHandler> handlers = network.energyHandlers().fillOrder();
        int remaining = amount;
        for (int i = 0; i < handlers.size() && remaining > 0; i++) {
            remaining -= handlers.get(i).insert(remaining, transaction);
        }
        return amount - remaining;
    }

    @Override
    public int extract(final int amount, final TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        final List<EnergyHandler> handlers = network.energyHandlers().drainOrder();
        int remaining = amount;
        for (int i = 0; i < handlers.size() && remaining > 0; i++) {
            remaining -= handlers.get(i).extract(remaining, transaction);
        }
        return amount - remaining;
    }
}
