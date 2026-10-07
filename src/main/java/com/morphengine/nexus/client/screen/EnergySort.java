package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.level.DeviceEnergyRow;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Comparator;
import java.util.Locale;

/**
 * The columns of the energy list, which the list can be sorted by: the name first, the biggest figures first for the
 * others.
 */
enum EnergySort {
    NAME(Comparator.comparing((DeviceEnergyRow row) -> row.name().getString())),
    DRAWN(Comparator.comparingLong((DeviceEnergyRow row) -> row.use().drawn()).reversed()),
    SUPPLIED(Comparator.comparingLong((DeviceEnergyRow row) -> row.use().supplied()).reversed()),
    TOLLS(Comparator.comparingLong((DeviceEnergyRow row) -> row.use().tolls()).reversed());

    private final Comparator<DeviceEnergyRow> order;

    EnergySort(final Comparator<DeviceEnergyRow> order) {
        this.order = order;
    }

    /**
     * @return the order of this column, rows that tie by name
     */
    Comparator<DeviceEnergyRow> comparator() {
        return this == NAME ? order : order.thenComparing(NAME.order);
    }

    /**
     * @return what the column shows, for the tooltip of its header
     */
    MutableComponent description() {
        return Component.translatable("gui.nexus.energy.column." + name().toLowerCase(Locale.ROOT) + ".tip");
    }

    Component label() {
        return Component.translatable("gui.nexus.energy.column." + name().toLowerCase(Locale.ROOT));
    }
}
