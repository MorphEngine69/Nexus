package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellMarks;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.item.BlueprintItem;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.DeviceBlockItem;
import com.morphengine.nexus.item.NetworkCardItem;
import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.item.TierUpgradeItem;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.item.VoidUpgradeItem;
import com.morphengine.nexus.item.WrenchItem;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachineMarks;
import com.morphengine.nexus.upgrade.NexusUpgradeType;
import com.morphengine.nexus.upgrade.UpgradeItem;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NexusItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nexus.MOD_ID);

    public static final DeferredItem<DeviceBlockItem> NEXUS = deviceItem(NexusBlocks.NEXUS);

    public static final DeferredItem<DeviceBlockItem> BASIC_ENERGY_CELL =
            energyCellItem(NexusBlocks.BASIC_ENERGY_CELL, EnergyCellTier.BASIC);

    public static final DeferredItem<DeviceBlockItem> ADVANCED_ENERGY_CELL =
            energyCellItem(NexusBlocks.ADVANCED_ENERGY_CELL, EnergyCellTier.ADVANCED);

    public static final DeferredItem<DeviceBlockItem> SUPERIOR_ENERGY_CELL =
            energyCellItem(NexusBlocks.SUPERIOR_ENERGY_CELL, EnergyCellTier.SUPERIOR);

    public static final DeferredItem<DeviceBlockItem> QUANTUM_ENERGY_CELL =
            energyCellItem(NexusBlocks.QUANTUM_ENERGY_CELL, EnergyCellTier.QUANTUM);

    /** Every Energy Cell, the smallest first. */
    public static final List<DeferredItem<DeviceBlockItem>> ENERGY_CELLS = List.of(
            BASIC_ENERGY_CELL, ADVANCED_ENERGY_CELL, SUPERIOR_ENERGY_CELL, QUANTUM_ENERGY_CELL);

    /** The items of the generators of every kind. */
    public static final Map<GeneratorKind, DeferredItem<DeviceBlockItem>> GENERATORS = generatorItems();

    /** The items of the machines of every kind, the lowest tier first. */
    public static final Map<MachineKind, List<DeferredItem<DeviceBlockItem>>> MACHINES = machineItems();

    public static final DeferredItem<DeviceBlockItem> STORAGE_VAULT = deviceItem(NexusBlocks.STORAGE_VAULT);

    public static final DeferredItem<DeviceBlockItem> TERMINAL = panelItem(NexusBlocks.TERMINAL);

    public static final DeferredItem<DeviceBlockItem> CRAFTING_TERMINAL = panelItem(NexusBlocks.CRAFTING_TERMINAL);

    public static final DeferredItem<DeviceBlockItem> EXTERNAL_VAULT = deviceItem(NexusBlocks.EXTERNAL_VAULT);

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

    public static final DeferredItem<TierUpgradeItem> ADVANCED_TIER_UPGRADE = tierUpgrade("advanced", 2);

    public static final DeferredItem<TierUpgradeItem> SUPERIOR_TIER_UPGRADE = tierUpgrade("superior", 3);

    public static final DeferredItem<TierUpgradeItem> QUANTUM_TIER_UPGRADE = tierUpgrade("quantum", 4);

    /** Every tier upgrade, the lowest first. */
    public static final List<DeferredItem<TierUpgradeItem>> TIER_UPGRADES = List.of(
            ADVANCED_TIER_UPGRADE, SUPERIOR_TIER_UPGRADE, QUANTUM_TIER_UPGRADE);

    public static final DeferredItem<WrenchItem> WRENCH =
            ITEMS.registerItem("wrench", properties -> new WrenchItem(properties.stacksTo(1)));

    public static final Map<DyeColor, DeferredItem<DeviceBlockItem>> CABLES = registerCables();

    /** Every Vault Cell, by what it stores and its size. */
    public static final Map<CellKind, Map<CellTier, DeferredItem<VaultCellItem>>> VAULT_CELLS = registerCells();

    public static final DeferredItem<UpgradeItem> SPEED_UPGRADE = registerUpgrade(UpgradeTypes.SPEED);
    public static final DeferredItem<UpgradeItem> STACK_UPGRADE = registerUpgrade(UpgradeTypes.STACK);
    public static final DeferredItem<UpgradeItem> REGULATOR_UPGRADE = registerUpgrade(UpgradeTypes.REGULATOR);
    public static final DeferredItem<UpgradeItem> CAPACITY_UPGRADE = registerUpgrade(UpgradeTypes.CAPACITY);
    public static final DeferredItem<UpgradeItem> EFFICIENCY_UPGRADE = registerUpgrade(UpgradeTypes.EFFICIENCY);
    public static final DeferredItem<UpgradeItem> BUFFER_UPGRADE = registerUpgrade(UpgradeTypes.BUFFER);
    public static final DeferredItem<UpgradeItem> RANGE_UPGRADE = registerUpgrade(UpgradeTypes.RANGE);
    public static final DeferredItem<UpgradeItem> FORTUNE_UPGRADE = registerUpgrade(UpgradeTypes.FORTUNE);
    public static final DeferredItem<UpgradeItem> SILK_TOUCH_UPGRADE = registerUpgrade(UpgradeTypes.SILK_TOUCH);
    public static final DeferredItem<UpgradeItem> AUTOCRAFTING_UPGRADE = registerUpgrade(UpgradeTypes.AUTOCRAFTING);
    public static final DeferredItem<UpgradeItem> CHUNK_LOADER_UPGRADE = registerUpgrade(UpgradeTypes.CHUNK_LOADER);
    public static final DeferredItem<UpgradeItem> VOID_UPGRADE = ITEMS.registerItem(
            UpgradeTypes.VOID.getId().getPath() + "_upgrade",
            properties -> new VoidUpgradeItem(UpgradeTypes.VOID, properties));

    /** Every upgrade, those that work first. */
    public static final List<DeferredItem<UpgradeItem>> UPGRADES = List.of(SPEED_UPGRADE, STACK_UPGRADE,
            REGULATOR_UPGRADE, CAPACITY_UPGRADE, EFFICIENCY_UPGRADE, BUFFER_UPGRADE, RANGE_UPGRADE, FORTUNE_UPGRADE,
            SILK_TOUCH_UPGRADE, AUTOCRAFTING_UPGRADE, CHUNK_LOADER_UPGRADE, VOID_UPGRADE);

    /** The still pose of the model that the item of a machine stands in: lit as if at work, nothing moving. */
    private static final String ITEM_POSE = "item";

    /** A terminal panel stands against the back of its block; as an item it is moved to the middle. */
    private static final float PANEL_ITEM_SHIFT_PIXELS = 7;
    private static final String CABLE_ASSET = "cable";
    private static final String ENERGY_CELL_ASSET = "energy_cell";

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

    /**
     * Ids read {@code <tier>_tier_upgrade}, such as {@code advanced_tier_upgrade}.
     */
    private static DeferredItem<TierUpgradeItem> tierUpgrade(final String tier, final int rank) {
        return ITEMS.registerItem(tier + "_tier_upgrade", properties -> new TierUpgradeItem(rank, properties));
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

    /**
     * The model of the cell is the same for every tier; the item shows only the marks of its own.
     */
    private static DeferredItem<DeviceBlockItem> energyCellItem(
            final DeferredBlock<EnergyCellBlock> block, final EnergyCellTier tier) {
        return deviceItem(block, DeviceBlockItem.Look.of(ENERGY_CELL_ASSET)
                .hiding(EnergyCellMarks.bonesOfOtherRanks(tier.rank())));
    }

    /**
     * The model of a machine is the same for every tier; the item shows only the marks of its own.
     */
    private static Map<MachineKind, List<DeferredItem<DeviceBlockItem>>> machineItems() {
        final Map<MachineKind, List<DeferredItem<DeviceBlockItem>>> machines = new EnumMap<>(MachineKind.class);
        for (MachineKind kind : MachineKind.values()) {
            final List<DeferredItem<DeviceBlockItem>> tiers = new ArrayList<>();
            final List<DeferredBlock<MachineBlock>> blocks = NexusBlocks.machineTiers(kind);
            for (int index = 0; index < blocks.size(); index++) {
                tiers.add(deviceItem(blocks.get(index), DeviceBlockItem.Look.of(kind.id())
                        .inSlot(kind.slotColor()).posed(ITEM_POSE).hiding(MachineMarks.bonesOfOtherRanks(index + 1))));
            }
            machines.put(kind, List.copyOf(tiers));
        }
        return Collections.unmodifiableMap(machines);
    }

    /**
     * The model of a generator is lit as if at work; one whose fire is painted on its texture shows the texture
     * of work.
     */
    private static Map<GeneratorKind, DeferredItem<DeviceBlockItem>> generatorItems() {
        final Map<GeneratorKind, DeferredItem<DeviceBlockItem>> generators = new EnumMap<>(GeneratorKind.class);
        for (GeneratorKind kind : GeneratorKind.values()) {
            final DeviceBlockItem.Look look = DeviceBlockItem.Look.of(kind.id()).posed(ITEM_POSE);
            generators.put(kind, deviceItem(NexusBlocks.GENERATORS.get(kind),
                    kind.hasPhaseTextures() ? look.withTextureSuffix("_active") : look));
        }
        return Collections.unmodifiableMap(generators);
    }

    private static DeferredItem<DeviceBlockItem> panelItem(final DeferredBlock<?> block) {
        return deviceItem(block, DeviceBlockItem.Look.of(block.getId().getPath()).shifted(PANEL_ITEM_SHIFT_PIXELS));
    }
}
