package com.morphengine.nexus.client.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/**
 * The category of a machine, named after the machine and shown with its icon.
 */
final class MachineCategory extends EmiRecipeCategory {

    private final Component name;

    MachineCategory(final ResourceLocation id, final String titleKey, final ItemLike machine) {
        super(id, EmiStack.of(machine));
        this.name = Component.translatable(titleKey);
    }

    @Override
    public Component getName() {
        return name;
    }
}
