package com.morphengine.nexus.registry;

import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.NexusCrystalItem;
import com.morphengine.nexus.metal.BlockFeel;
import com.morphengine.nexus.metal.VanillaMetal;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The plain items and blocks of the mod that are neither devices nor made of a metal: parts to craft with, alloys,
 * the dust and plates of vanilla metals, the bio resources and the ore of the Nexus.
 */
public final class NexusMaterials {

    public static final DeferredItem<Item> CORE = plain("core");
    public static final DeferredItem<Item> UPGRADE_BLANK = plain("upgrade_blank");
    public static final DeferredItem<Item> MACHINE_CASING = plain("machine_casing");
    public static final DeferredItem<Item> BATTERY = plain("battery");
    public static final DeferredItem<Item> SCREEN = plain("screen");
    public static final DeferredItem<Item> ANTENNA = plain("antenna");
    public static final DeferredItem<NexusCrystalItem> NEXUS_CRYSTAL =
            NexusItems.ITEMS.registerItem("nexus_crystal", NexusCrystalItem::new);

    public static final DeferredItem<Item> CELL_HOUSING = plain("cell_housing");

    /** The part of a Vault Cell of every size. */
    public static final Map<CellTier, DeferredItem<Item>> CELL_PARTS = registerCellParts();

    public static final DeferredItem<Item> VOLTSTEEL_INGOT = plain("voltsteel_ingot");
    public static final DeferredItem<Item> LUMEN_INGOT = plain("lumen_ingot");
    public static final DeferredItem<Item> AETHER_INGOT = plain("aether_ingot");

    /** The dust of a vanilla metal. */
    public static final Map<VanillaMetal, DeferredItem<Item>> VANILLA_DUSTS = registerForVanilla("dust");

    /** The plate of a vanilla metal. */
    public static final Map<VanillaMetal, DeferredItem<Item>> VANILLA_PLATES = registerForVanilla("plate");

    public static final DeferredItem<Item> POLYMER = plain("polymer");
    public static final DeferredItem<Item> BIOMASS = plain("biomass");

    public static final DeferredBlock<Block> NEXUS_ORE = registerNexusOre("nexus_ore", nexusOreFeel());
    public static final DeferredBlock<Block> DEEPSLATE_NEXUS_ORE =
            registerNexusOre("deepslate_nexus_ore", nexusOreFeel().inDeepslate());

    public static final DeferredItem<BlockItem> NEXUS_ORE_ITEM = NexusItems.ITEMS.registerSimpleBlockItem(NEXUS_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_NEXUS_ORE_ITEM =
            NexusItems.ITEMS.registerSimpleBlockItem(DEEPSLATE_NEXUS_ORE);

    private static final float NEXUS_ORE_HARDNESS = 3.0F;
    private static final float NEXUS_ORE_RESISTANCE = 3.0F;
    private static final int NEXUS_ORE_MIN_EXPERIENCE = 3;
    private static final int NEXUS_ORE_MAX_EXPERIENCE = 7;

    private NexusMaterials() {
    }

    /**
     * Loads the class, which registers its items and blocks; called once from the constructor of the mod.
     */
    public static void bootstrap() {
        // Nothing to do: the registrations are in the static fields.
    }

    private static DeferredItem<Item> plain(final String id) {
        return NexusItems.ITEMS.registerItem(id, Item::new);
    }

    private static Map<CellTier, DeferredItem<Item>> registerCellParts() {
        final Map<CellTier, DeferredItem<Item>> parts = new EnumMap<>(CellTier.class);
        for (CellTier tier : CellTier.values()) {
            parts.put(tier, plain("cell_part_" + tier.label()));
        }
        return Collections.unmodifiableMap(parts);
    }

    private static Map<VanillaMetal, DeferredItem<Item>> registerForVanilla(final String suffix) {
        final Map<VanillaMetal, DeferredItem<Item>> items = new EnumMap<>(VanillaMetal.class);
        for (VanillaMetal metal : VanillaMetal.values()) {
            items.put(metal, plain(metal.id() + "_" + suffix));
        }
        return Collections.unmodifiableMap(items);
    }

    private static BlockFeel nexusOreFeel() {
        return new BlockFeel(NEXUS_ORE_HARDNESS, NEXUS_ORE_RESISTANCE, SoundType.STONE, MapColor.STONE);
    }

    private static DeferredBlock<Block> registerNexusOre(final String id, final BlockFeel feel) {
        return NexusBlocks.BLOCKS.registerBlock(id,
                properties -> new DropExperienceBlock(
                        UniformInt.of(NEXUS_ORE_MIN_EXPERIENCE, NEXUS_ORE_MAX_EXPERIENCE), properties),
                feel.apply(BlockBehaviour.Properties.of()));
    }
}
