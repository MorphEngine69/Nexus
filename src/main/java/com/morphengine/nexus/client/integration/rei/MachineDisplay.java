package com.morphengine.nexus.client.integration.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * One recipe of a machine as REI shows it: what goes in and what comes out, in the category of the machine.
 */
final class MachineDisplay extends BasicDisplay {

    private final CategoryIdentifier<MachineDisplay> category;

    MachineDisplay(
            final CategoryIdentifier<MachineDisplay> category, final List<EntryIngredient> inputs,
            final List<EntryIngredient> outputs, final Identifier recipeId) {
        super(inputs, outputs, Optional.of(recipeId));
        this.category = category;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    /**
     * @return {@code null}: the display is made on the client from the recipes it knows and is never synced
     */
    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
