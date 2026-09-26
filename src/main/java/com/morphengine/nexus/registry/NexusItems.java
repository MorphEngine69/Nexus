package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.VaultCellItem;
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

    public static final DeferredItem<BlockItem> STORAGE_VAULT =
            ITEMS.registerSimpleBlockItem(NexusBlocks.STORAGE_VAULT);

    public static final DeferredItem<BlockItem> TERMINAL = ITEMS.registerSimpleBlockItem(NexusBlocks.TERMINAL);

    public static final DeferredItem<BlockItem> CRAFTING_TERMINAL =
            ITEMS.registerSimpleBlockItem(NexusBlocks.CRAFTING_TERMINAL);

    public static final Map<DyeColor, DeferredItem<BlockItem>> CABLES = registerCables();

    /** Every Vault Cell, by what it stores and its size. */
    public static final Map<CellKind, Map<CellTier, DeferredItem<VaultCellItem>>> VAULT_CELLS = registerCells();

    private NexusItems() {
    }

    /**
     * Ids read {@code <kind>_vault_cell_<size>}, such as {@code item_vault_cell_4k}.
     */
    public static String cellName(final CellKind kind, final CellTier tier) {
        return kind.getSerializedName() + "_vault_cell_" + tier.label();
    }

    private static Map<CellKind, Map<CellTier, DeferredItem<VaultCellItem>>> registerCells() {
        final Map<CellKind, Map<CellTier, DeferredItem<VaultCellItem>>> cells = new EnumMap<>(CellKind.class);
        for (CellKind kind : CellKind.values()) {
            final Map<CellTier, DeferredItem<VaultCellItem>> tiers = new EnumMap<>(CellTier.class);
            for (CellTier tier : CellTier.values()) {
                tiers.put(tier, ITEMS.registerItem(cellName(kind, tier),
                        properties -> new VaultCellItem(kind, tier, properties)));
            }
            cells.put(kind, Collections.unmodifiableMap(tiers));
        }
        return Collections.unmodifiableMap(cells);
    }

    private static Map<DyeColor, DeferredItem<BlockItem>> registerCables() {
        final Map<DyeColor, DeferredItem<BlockItem>> cables = new EnumMap<>(DyeColor.class);
        for (Map.Entry<DyeColor, DeferredBlock<CableBlock>> cable : NexusBlocks.CABLES.entrySet()) {
            cables.put(cable.getKey(), ITEMS.registerSimpleBlockItem(cable.getValue()));
        }
        return Collections.unmodifiableMap(cables);
    }
}
