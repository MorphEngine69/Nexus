package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.item.CellContents;
import com.morphengine.nexus.item.CellFilter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class NexusDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nexus.MOD_ID);

    /** What a Vault Cell holds. */
    public static final Supplier<DataComponentType<CellContents>> CELL_CONTENTS = COMPONENTS.registerComponentType(
            "cell_contents",
            builder -> builder.persistent(CellContents.CODEC).networkSynchronized(CellContents.STREAM_CODEC));

    /** The whitelist or blacklist set on a Vault Cell. */
    public static final Supplier<DataComponentType<CellFilter>> CELL_FILTER = COMPONENTS.registerComponentType(
            "cell_filter",
            builder -> builder.persistent(CellFilter.CODEC).networkSynchronized(CellFilter.STREAM_CODEC));

    private NexusDataComponents() {
    }
}
