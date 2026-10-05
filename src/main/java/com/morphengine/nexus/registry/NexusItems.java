package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.item.BlueprintItem;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.DeviceBlockItem;
import com.morphengine.nexus.item.NetworkCardItem;
import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.item.WrenchItem;
import com.morphengine.nexus.upgrade.NexusUpgradeType;
import com.morphengine.nexus.upgrade.UpgradeItem;
import com.morphengine.nexus.upgrade.UpgradeTypes;
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

    public static final DeferredItem<DeviceBlockItem> NEXUS = deviceItem(NexusBlocks.NEXUS);

    public static final DeferredItem<DeviceBlockItem> BASIC_ENERGY_CELL =
            deviceItem(NexusBlocks.BASIC_ENERGY_CELL, "energy_cell");

    public static final DeferredItem<DeviceBlockItem> COAL_GENERATOR = deviceItem(
            NexusBlocks.COAL_GENERATOR, DeviceBlockItem.Look.of("coal_generator").posed("burning"));

    public static final DeferredItem<DeviceBlockItem> STORAGE_VAULT = deviceItem(NexusBlocks.STORAGE_VAULT);

    public static final DeferredItem<DeviceBlockItem> TERMINAL = panelItem(NexusBlocks.TERMINAL);

    public static final DeferredItem<DeviceBlockItem> CRAFTING_TERMINAL = panelItem(NexusBlocks.CRAFTING_TERMINAL);

    public static final DeferredItem<DeviceBlockItem> PULLER = deviceItem(NexusBlocks.PULLER);

    public static final DeferredItem<DeviceBlockItem> PUSHER = deviceItem(NexusBlocks.PUSHER);

    public static final DeferredItem<DeviceBlockItem> PLACER = deviceItem(NexusBlocks.PLACER);

    public static final DeferredItem<DeviceBlockItem> REMOVER = deviceItem(NexusBlocks.REMOVER);

    public static final DeferredItem<DeviceBlockItem> ASSEMBLER = deviceItem(NexusBlocks.ASSEMBLER);

    public static final DeferredItem<DeviceBlockItem> CRAFTING_MONITOR = deviceItem(NexusBlocks.CRAFTING_MONITOR);

    public static final DeferredItem<DeviceBlockItem> BLUEPRINT_TERMINAL =
            panelItem(NexusBlocks.BLUEPRINT_TERMINAL);

    public static final DeferredItem<BlueprintItem> BLUEPRINT = ITEMS.registerItem("blueprint", BlueprintItem::new);

    public static final DeferredItem<DeviceBlockItem> NETWORK_TRANSMITTER =
            deviceItem(NexusBlocks.NETWORK_TRANSMITTER);

    public static final DeferredItem<DeviceBlockItem> NETWORK_RECEIVER =
            deviceItem(NexusBlocks.NETWORK_RECEIVER);

    public static final DeferredItem<DeviceBlockItem> NEXUS_LINK = deviceItem(NexusBlocks.NEXUS_LINK);

    public static final DeferredItem<NetworkCardItem> NETWORK_CARD =
            ITEMS.registerItem("network_card", properties -> new NetworkCardItem(properties.stacksTo(1)));

    public static final DeferredItem<NexusTerminalItem> NEXUS_TERMINAL =
            ITEMS.registerItem("nexus_terminal", properties -> new NexusTerminalItem(properties.stacksTo(1)));

    public static final DeferredItem<WrenchItem> WRENCH =
            ITEMS.registerItem("wrench", properties -> new WrenchItem(properties.stacksTo(1)));

    public static final Map<DyeColor, DeferredItem<DeviceBlockItem>> CABLES = registerCables();

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
    public static final DeferredItem<UpgradeItem> CHUNK_LOADER_UPGRADE = registerUpgrade(UpgradeTypes.CHUNK_LOADER);

    /** Every upgrade, those that work first. */
    public static final List<DeferredItem<UpgradeItem>> UPGRADES = List.of(SPEED_UPGRADE, STACK_UPGRADE,
            REGULATOR_UPGRADE, CAPACITY_UPGRADE, RANGE_UPGRADE, FORTUNE_UPGRADE, SILK_TOUCH_UPGRADE,
            AUTOCRAFTING_UPGRADE, CHUNK_LOADER_UPGRADE);

    /** A terminal panel stands against the back of its block; as an item it is moved to the middle. */
    private static final float PANEL_ITEM_SHIFT_PIXELS = 7;
    private static final String CABLE_ASSET = "cable";

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

    /**
     * A cable is drawn from its GeckoLib model only as an item in a slot; in the hand and in the world it is the block
     * model, which a long run of cable needs, since a cable has no block entity.
     */
    private static Map<DyeColor, DeferredItem<DeviceBlockItem>> registerCables() {
        final Map<DyeColor, DeferredItem<DeviceBlockItem>> cables = new EnumMap<>(DyeColor.class);
        for (Map.Entry<DyeColor, DeferredBlock<CableBlock>> cable : NexusBlocks.CABLES.entrySet()) {
            cables.put(cable.getKey(), deviceItem(
                    cable.getValue(), DeviceBlockItem.Look.of(CABLE_ASSET).colored(cable.getKey())));
        }
        return Collections.unmodifiableMap(cables);
    }

    private static DeferredItem<DeviceBlockItem> deviceItem(final DeferredBlock<?> block) {
        return deviceItem(block, block.getId().getPath());
    }

    private static DeferredItem<DeviceBlockItem> deviceItem(final DeferredBlock<?> block, final String asset) {
        return deviceItem(block, DeviceBlockItem.Look.of(asset));
    }

    private static DeferredItem<DeviceBlockItem> deviceItem(
            final DeferredBlock<?> block, final DeviceBlockItem.Look look) {
        return ITEMS.registerItem(block.getId().getPath(), properties -> new DeviceBlockItem(
                block.get(), properties.useBlockDescriptionPrefix(), look));
    }

    private static DeferredItem<DeviceBlockItem> panelItem(final DeferredBlock<?> block) {
        return deviceItem(block, DeviceBlockItem.Look.of(block.getId().getPath()).shifted(PANEL_ITEM_SHIFT_PIXELS));
    }
}
