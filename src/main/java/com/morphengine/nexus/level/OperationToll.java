package com.morphengine.nexus.level;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.config.NexusConfig;
import com.morphengine.nexus.energy.DeviceEnergyMeter;
import com.morphengine.nexus.energy.EnergyScale;
import com.morphengine.nexus.energy.OperationKind;
import com.morphengine.nexus.energy.OperationPrice;
import com.morphengine.nexus.energy.OperationUpgrades;

/**
 * Takes the FE an operation costs from the energy pool of the network. A device asks whether the network
 * {@linkplain #affords affords} the operation before it does it, and {@linkplain #charge charges} it once it did
 * something, so that a device with nothing to do costs nothing. A price may be more than the buffers of the pool give
 * in one go, so it is paid in as many draws as it takes. Server thread only.
 */
public final class OperationToll {

    private static final int MAX_DRAWS = 64;

    private OperationToll() {
    }

    /**
     * @param upgrades the upgrades of the device that works, which change what the operation costs
     * @return whether the pool holds the whole price of one operation
     */
    public static boolean affords(
            final NetworkController network, final OperationKind kind, final OperationUpgrades upgrades) {
        return network.energy().stored() >= priceOf(network, kind, upgrades);
    }

    /**
     * Takes the price of one operation from the pool, as far as the pool holds it, in as many draws as the buffers
     * need to give it.
     *
     * @param upgrades the upgrades of the device that worked
     * @param payer    the meter of the device that worked, which records what was taken
     */
    public static void charge(
            final NetworkController network, final OperationKind kind, final OperationUpgrades upgrades,
            final DeviceEnergyMeter payer) {
        final EnergyBuffer pool = network.energy();
        final long price = priceOf(network, kind, upgrades);
        long left = price;
        for (int draw = 0; draw < MAX_DRAWS && left > 0; draw++) {
            final long taken = pool.extract(left, Action.EXECUTE);
            if (taken <= 0) {
                break;
            }
            left -= taken;
        }
        if (left < price) {
            payer.recordToll(price - left);
        }
    }

    private static long priceOf(
            final NetworkController network, final OperationKind kind, final OperationUpgrades upgrades) {
        return EnergyScale.priceOf(OperationPrice.of(kind, network.statistics().devices(), upgrades),
                NexusConfig.operationPricePercent());
    }
}
