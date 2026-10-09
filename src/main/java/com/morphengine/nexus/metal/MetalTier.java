package com.morphengine.nexus.metal;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.Objects;

/**
 * Durability, speed and level of the tools of a metal.
 *
 * @param incorrectBlocks the blocks that these tools do not drop anything from, which sets their level
 * @param uses            durability of a tool
 * @param speed           how fast the tools mine
 * @param damageBonus     the damage that the tier adds to the baseline of each tool
 * @param enchantability  how well the tools take enchantments
 * @param repairItems     the items that repair the tools
 */
public record MetalTier(TagKey<Block> incorrectBlocks, int uses, float speed, float damageBonus, int enchantability,
                        TagKey<Item> repairItems) implements Tier {

    public MetalTier {
        Objects.requireNonNull(incorrectBlocks, "incorrectBlocks");
        Objects.requireNonNull(repairItems, "repairItems");
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlocks;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(repairItems);
    }
}
