package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class NexusItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nexus.MOD_ID);

    public static final DeferredItem<BlockItem> NEXUS = ITEMS.registerSimpleBlockItem(NexusBlocks.NEXUS);

    public static final DeferredItem<BlockItem> BASIC_ENERGY_CELL =
            ITEMS.registerSimpleBlockItem(NexusBlocks.BASIC_ENERGY_CELL);

    public static final DeferredItem<BlockItem> COAL_GENERATOR =
            ITEMS.registerSimpleBlockItem(NexusBlocks.COAL_GENERATOR);

    public static final Map<DyeColor, DeferredItem<BlockItem>> CABLES = registerCables();

    private NexusItems() {
    }

    private static Map<DyeColor, DeferredItem<BlockItem>> registerCables() {
        final Map<DyeColor, DeferredItem<BlockItem>> cables = new EnumMap<>(DyeColor.class);
        for (Map.Entry<DyeColor, DeferredBlock<CableBlock>> cable : NexusBlocks.CABLES.entrySet()) {
            cables.put(cable.getKey(), ITEMS.registerSimpleBlockItem(cable.getValue()));
        }
        return Collections.unmodifiableMap(cables);
    }
}
