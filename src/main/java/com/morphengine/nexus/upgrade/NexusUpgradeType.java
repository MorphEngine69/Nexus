package com.morphengine.nexus.upgrade;

import com.morphengine.nexus.api.upgrade.UpgradeType;

/**
 * An upgrade of Nexus as the game registers it. Some kinds are registered
 * ahead of the devices that will take them; such a kind is in development: no
 * device takes it yet, and its item says so.
 */
public final class NexusUpgradeType implements UpgradeType {

    private final boolean inDevelopment;

    private NexusUpgradeType(final boolean inDevelopment) {
        this.inDevelopment = inDevelopment;
    }

    /**
     * @return a kind some device already takes
     */
    public static NexusUpgradeType working() {
        return new NexusUpgradeType(false);
    }

    /**
     * @return a kind registered ahead of the devices that will take it
     */
    public static NexusUpgradeType inDevelopment() {
        return new NexusUpgradeType(true);
    }

    public boolean isInDevelopment() {
        return inDevelopment;
    }
}
