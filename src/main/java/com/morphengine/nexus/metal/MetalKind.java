package com.morphengine.nexus.metal;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A metal of the mod that comes out of the ground: its ore, its ingot, its tools and its armor. A new metal is a new
 * constant here, a place in {@code NexusMetals} and the files of its assets and data; no other code changes.
 *
 * @param id            the name in the ids of every item and block of the metal, such as {@code steel}
 * @param tools         durability, speed and the level of the tools
 * @param armor         the armor the metal makes
 * @param handling      the attack baselines of the axe and the hoe
 * @param blocks        how the ore and the block of the metal break
 * @param fireResistant whether the items of the metal do not burn in fire and lava, as netherite does not
 * @param inStone       whether the metal has an ore in stone and deepslate; if not, it has one ore, in the Nether
 */
public record MetalKind(String id, MetalTier tools, MetalArmor armor, ToolHandling handling,
                        MetalBlocks blocks, boolean fireResistant, boolean inStone) {

    private static final String STEEL_ID = "steel";
    private static final String COBALT_ID = "cobalt";
    private static final String MITHRIL_ID = "mithril";
    private static final String HELLSTEEL_ID = "hellsteel";

    /** As iron, but sturdier. */
    public static final MetalKind STEEL = new MetalKind(STEEL_ID,
            tier(STEEL_ID, BlockTags.INCORRECT_FOR_IRON_TOOL, 300, 6.0F, 2.0F, 14),
            armor(STEEL_ID, 17, defense(2, 5, 6, 2, 5), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F),
            new ToolHandling(6.0F, -3.1F, -2.0F, -1.0F),
            new MetalBlocks(new BlockFeel(3.0F, 3.0F, SoundType.STONE, MapColor.STONE),
                    new BlockFeel(5.0F, 6.0F, SoundType.METAL, MapColor.METAL),
                    ConstantInt.of(0)),
            false, true);

    /** Mined as gold, but made like diamond and a little more durable. */
    public static final MetalKind COBALT = new MetalKind(COBALT_ID,
            tier(COBALT_ID, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.0F, 3.0F, 12),
            armor(COBALT_ID, 36, defense(3, 6, 8, 3, 11), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.0F),
            new ToolHandling(5.0F, -3.0F, -3.0F, 0.0F),
            new MetalBlocks(new BlockFeel(3.0F, 3.0F, SoundType.STONE, MapColor.STONE),
                    new BlockFeel(3.0F, 6.0F, SoundType.METAL, MapColor.METAL),
                    ConstantInt.of(0)),
            false, true);

    /** Mined as diamond; its tools and armor are almost netherite. */
    public static final MetalKind MITHRIL = new MetalKind(MITHRIL_ID,
            tier(MITHRIL_ID, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1950, 8.8F, 3.8F, 14),
            armor(MITHRIL_ID, 36, defense(3, 6, 8, 3, 19), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.05F),
            new ToolHandling(5.0F, -3.0F, -3.8F, 0.0F),
            new MetalBlocks(new BlockFeel(3.0F, 3.0F, SoundType.STONE, MapColor.STONE),
                    new BlockFeel(5.0F, 6.0F, SoundType.METAL, MapColor.DIAMOND),
                    UniformInt.of(3, 7)),
            false, true);

    /** Found in the Nether like ancient debris; better than netherite. */
    public static final MetalKind HELLSTEEL = new MetalKind(HELLSTEEL_ID,
            tier(HELLSTEEL_ID, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2400, 10.0F, 4.5F, 16),
            armor(HELLSTEEL_ID, 40, defense(3, 6, 8, 3, 19), 16, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F,
                    0.15F),
            new ToolHandling(5.0F, -3.0F, -4.5F, 0.0F),
            new MetalBlocks(new BlockFeel(30.0F, 1200.0F, SoundType.ANCIENT_DEBRIS, MapColor.COLOR_BLACK),
                    new BlockFeel(50.0F, 1200.0F, SoundType.NETHERITE_BLOCK, MapColor.COLOR_BLACK),
                    ConstantInt.of(0)),
            true, false);

    /** Every metal, the softest first. */
    public static final List<MetalKind> ALL = List.of(STEEL, COBALT, MITHRIL, HELLSTEEL);

    public MetalKind {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tools, "tools");
        Objects.requireNonNull(armor, "armor");
        Objects.requireNonNull(handling, "handling");
        Objects.requireNonNull(blocks, "blocks");
    }

    /**
     * @return the properties with what every item of the metal has: nothing more, or resistance to fire
     */
    public Item.Properties shape(final Item.Properties properties) {
        return fireResistant ? properties.fireResistant() : properties;
    }

    private static MetalTier tier(final String id, final TagKey<Block> incorrectBlocks, final int durability,
                                  final float speed, final float damageBonus, final int enchantability) {
        return new MetalTier(incorrectBlocks, durability, speed, damageBonus, enchantability,
                itemTag(id + "_tool_materials"));
    }

    private static MetalArmor armor(final String id, final int durabilityFactor,
                                    final Map<ArmorItem.Type, Integer> defense, final int enchantability,
                                    final Holder<SoundEvent> equipSound, final float toughness,
                                    final float knockbackResistance) {
        return new MetalArmor(durabilityFactor, defense, enchantability, equipSound, toughness, knockbackResistance,
                itemTag("repairs_" + id + "_armor"));
    }

    private static Map<ArmorItem.Type, Integer> defense(final int boots, final int leggings, final int chestplate,
                                                        final int helmet, final int body) {
        final Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.HELMET, helmet);
        return defense;
    }

    private static TagKey<Item> itemTag(final String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, name));
    }
}
