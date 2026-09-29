package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.upgrade.NexusUpgradeType;
import com.morphengine.nexus.upgrade.UpgradeItem;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
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

    public static final DeferredItem<BlockItem> PULLER = ITEMS.registerSimpleBlockItem(NexusBlocks.PULLER);

    public static final DeferredItem<BlockItem> PUSHER = ITEMS.registerSimpleBlockItem(NexusBlocks.PUSHER);

    public static final Map<DyeColor, DeferredItem<BlockItem>> CABLES = registerCables();

    /** Every Vault Cell, by what it stores and its size. */
    public static final Map<CellKind, Map<CellTier, DeferredItem<VaultCellItem>>> VAULT_CELLS = registerCells();

    public static final DeferredItem<UpgradeItem> SPEED_UPGRADE = registerUpgrade(UpgradeTypes.SPEED);
    public static final DeferredItem<UpgradeItem> STACK_UPGRADE = registerUpgrade(UpgradeTypes.STACK);
    public static final DeferredItem<UpgradeItem> REGULATOR_UPGRADE = registerUpgrade(UpgradeTypes.REGULATOR);
    public static final DeferredItem<UpgradeItem> CAPACITY_UPGRADE = registerUpgrade(UpgradeTypes.CAPACITY);
    public static final DeferredItem<UpgradeItem> RANGE_UPGRADE = registerUpgrade(UpgradeTypes.RANGE);
    public static final DeferredItem<UpgradeItem> FORTUNE_UPGRADE = registerUpgrade(UpgradeTypes.FORTUNE);
    public static final DeferredItem<UpgradeItem> SILK_TOUCH_UPGRADE = registerUpgrade(UpgradeTypes.SILK_TOUCH);
    public static final DeferredItem<UpgradeItem> AUTOCRAFTING_UPGRADE = registerUpgrade(UpgradeTypes.AUTOCRAFTING);

    /** Every upgrade, those that work first. */
    public static final List<DeferredItem<UpgradeItem>> UPGRADES = List.of(SPEED_UPGRADE, STACK_UPGRADE,
            REGULATOR_UPGRADE, CAPACITY_UPGRADE, RANGE_UPGRADE, FORTUNE_UPGRADE, SILK_TOUCH_UPGRADE,
            AUTOCRAFTING_UPGRADE);

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

    /**
     * Ids read {@code <kind>_upgrade}, such as {@code speed_upgrade}.
     */
    private static DeferredItem<UpgradeItem> registerUpgrade(
            final DeferredHolder<NexusUpgradeType, NexusUpgradeType> type) {
        return ITEMS.registerItem(type.getId().getPath() + "_upgrade", properties -> new UpgradeItem(type, properties));
    }

    private static Map<DyeColor, DeferredItem<BlockItem>> registerCables() {
        final Map<DyeColor, DeferredItem<BlockItem>> cables = new EnumMap<>(DyeColor.class);
        for (Map.Entry<DyeColor, DeferredBlock<CableBlock>> cable : NexusBlocks.CABLES.entrySet()) {
            cables.put(cable.getKey(), ITEMS.registerSimpleBlockItem(cable.getValue()));
        }
        return Collections.unmodifiableMap(cables);
    }
}
