package com.morphengine.nexus.blueprint;

import com.morphengine.nexus.api.resource.ResourceAmount;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

/**
 * What a crafting grid makes, as {@link GridCrafting} works it out.
 *
 * @param outputs  the result, then what the ingredients leave behind; empty
 *                 when no recipe matches
 * @param accepted per filled slot index, what the recipe accepts there
 */
public record GridRecipe(List<ResourceAmount> outputs, Map<Integer, Ingredient> accepted) {

    public static final GridRecipe NONE = new GridRecipe(List.of(), Map.of());

    public GridRecipe {
        outputs = List.copyOf(outputs);
        accepted = Map.copyOf(accepted);
    }
}
