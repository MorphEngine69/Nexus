package com.morphengine.nexus.world;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * The tool a Remover breaks blocks with, as its upgrades make it: a pickaxe
 * that takes whatever a pickaxe takes, with Fortune of the level its Fortune
 * Upgrades add up to, or with Silk Touch, which wins over Fortune.
 *
 * @param fortune   level of Fortune, from zero
 * @param silkTouch whether it has Silk Touch
 */
public record HarvestTool(int fortune, boolean silkTouch) {

    public static final HarvestTool PLAIN = new HarvestTool(0, false);

    public HarvestTool {
        if (fortune < 0) {
            throw new IllegalArgumentException("fortune must not be negative: " + fortune);
        }
    }

    /**
     * @return a new stack of the tool, enchanted as it is
     */
    ItemStack toStack(final ServerLevel level) {
        final ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        final Registry<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (silkTouch) {
            tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        } else if (fortune > 0) {
            tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), fortune);
        }
        return tool;
    }
}
