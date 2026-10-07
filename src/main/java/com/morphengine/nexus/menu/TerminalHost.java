package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.entity.BlueprintEncoder;
import com.morphengine.nexus.block.entity.TerminalCraftingGrid;
import com.morphengine.nexus.energy.DeviceEnergyMeter;
import com.morphengine.nexus.energy.OperationKind;
import com.morphengine.nexus.energy.OperationUpgrades;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.OperationToll;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.terminal.TerminalStatus;
import org.jspecify.annotations.Nullable;

/**
 * What a terminal menu works for: a terminal block, or a Nexus Terminal in a
 * player's hand. It reaches a network's storage while it is {@link
 * TerminalStatus#ONLINE}, keeps the player's settings, and may keep a crafting
 * grid or a Blueprint encoder. Server side only.
 */
public interface TerminalHost {

    TerminalStatus status();

    /**
     * @return the network while the terminal is online; {@code null} otherwise
     */
    @Nullable NetworkController onlineNetwork();

    /**
     * @return whether the network can pay for one take out of it now; an offline terminal takes nothing anyway
     */
    default boolean affordsTake() {
        final NetworkController network = onlineNetwork();
        return network == null || OperationToll.affords(network, OperationKind.TERMINAL_TAKE, OperationUpgrades.NONE);
    }

    /**
     * Takes the price of one take out of the network from its energy.
     */
    default void chargeTake() {
        final NetworkController network = onlineNetwork();
        if (network != null) {
            OperationToll.charge(network, OperationKind.TERMINAL_TAKE, OperationUpgrades.NONE, tollPayer(network));
        }
    }

    /**
     * @return the meter that records the tolls of this terminal: the Nexus pays for a terminal that has no block
     */
    default DeviceEnergyMeter tollPayer(final NetworkController network) {
        return network.component(NetworkComponentTypes.ENERGY_ACCOUNT).portableTerminals();
    }

    /**
     * @return everything the network holds, its energy pool included, while
     *         online; {@code null} otherwise
     */
    @Nullable Storage onlineResources();

    /**
     * @return the network's storage while online; {@code null} otherwise
     */
    @Nullable NetworkStorage onlineStorage();

    /**
     * @return the network's autocrafting while online; {@code null} otherwise
     */
    @Nullable AutocraftingComponent onlineAutocrafting();

    /**
     * @return name and color of the network; {@code null} when there is none
     */
    @Nullable NetworkBadge networkBadge();

    void changeSettings(TerminalSettings settings);

    /**
     * @return the crafting grid; {@code null} for a host without one
     */
    @Nullable TerminalCraftingGrid craftingGrid();

    /**
     * @return the Blueprint encoder; {@code null} for a host without one
     */
    @Nullable BlueprintEncoder encoder();
}
