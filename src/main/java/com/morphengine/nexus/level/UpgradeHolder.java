package com.morphengine.nexus.level;

import net.minecraft.world.Container;

/**
 * A block entity that holds upgrades in slots of its own.
 */
public interface UpgradeHolder {

    /**
     * @return the slots the upgrades are in
     */
    Container upgrades();
}
