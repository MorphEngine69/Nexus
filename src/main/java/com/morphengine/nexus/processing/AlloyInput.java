package com.morphengine.nexus.processing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * What lies in the input slots of an Alloy Smelter, each item once with all that there is of it.
 */
public record AlloyInput(List<ItemStack> stacks) implements RecipeInput {

    public AlloyInput {
        stacks = List.copyOf(stacks);
    }

    @Override
    public ItemStack getItem(final int index) {
        return stacks.get(index);
    }

    @Override
    public int size() {
        return stacks.size();
    }
}
