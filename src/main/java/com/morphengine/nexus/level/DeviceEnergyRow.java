package com.morphengine.nexus.level;

import com.morphengine.nexus.api.network.DeviceEnergyUse;
import com.morphengine.nexus.api.network.DeviceRole;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * One device in the energy statistics of a network.
 *
 * @param position where the device stands
 * @param name     what the device is called, its own name when the player gave it one
 * @param role     what the device does
 * @param use      what it did with energy over the last measured period
 */
public record DeviceEnergyRow(GlobalPos position, Component name, DeviceRole role, DeviceEnergyUse use) {

    public DeviceEnergyRow {
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(use, "use must not be null");
    }
}
