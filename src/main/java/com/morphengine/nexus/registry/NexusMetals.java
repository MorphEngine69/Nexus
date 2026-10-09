package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.metal.BlockFeel;
import com.morphengine.nexus.metal.MetalKind;
import com.morphengine.nexus.metal.MetalPart;
import com.morphengine.nexus.metal.ToolPart;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything every {@link MetalKind} makes: raw metal, ingot, nugget, plate, dust, the ores, the block, the tools and
 * the armor.
 */
public final class NexusMetals {

    /** The armor materials of the metals; the game asks for them by registry. */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Nexus.MOD_ID);

    /** Every metal and what it made, the softest first. */
    public static final Map<MetalKind, MetalSet> SETS = registerAll();

    private NexusMetals() {
    }

    /**
     * Loads the class, which registers its items and blocks; called once from the constructor of the mod.
     */
    public static void bootstrap() {
        // Nothing to do: the registrations are in the static fields.
    }

    /**
     * @return the set of {@code metal}
     */
    public static MetalSet of(final MetalKind metal) {
        return SETS.get(metal);
    }

    private static Map<MetalKind, MetalSet> registerAll() {
        final Map<MetalKind, MetalSet> sets = new LinkedHashMap<>();
        for (MetalKind metal : MetalKind.ALL) {
            sets.put(metal, register(metal));
        }
        return Collections.unmodifiableMap(sets);
    }

    private static MetalSet register(final MetalKind metal) {
        final List<DeferredBlock<Block>> ores = new ArrayList<>();
        ores.add(registerOre(metal.id() + "_ore", metal, metal.blocks().ore()));
        if (metal.inStone()) {
            ores.add(registerOre("deepslate_" + metal.id() + "_ore", metal, metal.blocks().ore().inDeepslate()));
        }
        final DeferredBlock<Block> storage = NexusBlocks.BLOCKS.registerBlock(
                metal.id() + "_block", Block::new, metal.blocks().storage().apply(BlockBehaviour.Properties.of()));
        final DeferredHolder<ArmorMaterial, ArmorMaterial> material =
                ARMOR_MATERIALS.register(metal.id(), () -> metal.armor().material(metal.id()));
        return new MetalSet(metal, registerParts(metal), registerTools(metal), registerArmor(metal, material),
                List.copyOf(ores), ores.stream().map(ore -> registerBlockItem(ore, metal)).toList(),
                storage, registerBlockItem(storage, metal));
    }

    private static DeferredBlock<Block> registerOre(final String id, final MetalKind metal, final BlockFeel feel) {
        return NexusBlocks.BLOCKS.registerBlock(id,
                properties -> new DropExperienceBlock(metal.blocks().experience(), properties),
                feel.apply(BlockBehaviour.Properties.of()));
    }

    private static DeferredItem<BlockItem> registerBlockItem(final DeferredBlock<Block> block, final MetalKind metal) {
        return NexusItems.ITEMS.registerSimpleBlockItem(block, metal.shape(new Item.Properties()));
    }

    private static Map<MetalPart, DeferredItem<Item>> registerParts(final MetalKind metal) {
        final Map<MetalPart, DeferredItem<Item>> parts = new EnumMap<>(MetalPart.class);
        for (MetalPart part : MetalPart.values()) {
            parts.put(part, NexusItems.ITEMS.registerItem(part.idFor(metal),
                    properties -> new Item(metal.shape(properties))));
        }
        return Collections.unmodifiableMap(parts);
    }

    private static Map<ToolPart, DeferredItem<Item>> registerTools(final MetalKind metal) {
        final Map<ToolPart, DeferredItem<Item>> tools = new EnumMap<>(ToolPart.class);
        for (ToolPart tool : ToolPart.values()) {
            tools.put(tool, NexusItems.ITEMS.registerItem(
                    tool.idFor(metal), properties -> tool.create(metal, metal.shape(properties))));
        }
        return Collections.unmodifiableMap(tools);
    }

    private static Map<ArmorItem.Type, DeferredItem<Item>> registerArmor(
            final MetalKind metal, final DeferredHolder<ArmorMaterial, ArmorMaterial> material) {
        final Map<ArmorItem.Type, DeferredItem<Item>> armor = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type piece : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS,
                ArmorItem.Type.BOOTS)) {
            armor.put(piece, NexusItems.ITEMS.registerItem(metal.id() + "_" + piece.getName(),
                    properties -> new ArmorItem(material, piece, metal.shape(properties)
                            .durability(piece.getDurability(metal.armor().durabilityFactor())))));
        }
        return Collections.unmodifiableMap(armor);
    }

    /**
     * What one metal made.
     *
     * @param ores      the ore blocks of the metal, the one in stone first, then the one in deepslate if there is one
     * @param oreItems  the items of {@code ores}, in the same order
     */
    public record MetalSet(MetalKind kind, Map<MetalPart, DeferredItem<Item>> parts,
                           Map<ToolPart, DeferredItem<Item>> tools, Map<ArmorItem.Type, DeferredItem<Item>> armor,
                           List<DeferredBlock<Block>> ores, List<DeferredItem<BlockItem>> oreItems,
                           DeferredBlock<Block> storageBlock, DeferredItem<BlockItem> storageBlockItem) {

        public DeferredItem<Item> part(final MetalPart part) {
            return parts.get(part);
        }
    }
}
