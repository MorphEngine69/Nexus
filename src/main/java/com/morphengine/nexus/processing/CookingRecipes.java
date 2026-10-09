package com.morphengine.nexus.processing;

import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;
import java.util.Optional;

/**
 * The cooking recipes of the game, smelting, blasting and smoking, for a machine that does all three: the first of them
 * that has a recipe for the item wins, and the time of the recipe is the time of the work.
 */
public final class CookingRecipes implements LevelRecipes {

    private static final List<RecipeType<? extends AbstractCookingRecipe>> TYPES =
            List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING);

    private final long energyPerTick;

    /**
     * @param energyPerTick FE the work takes in each tick at normal speed, positive
     */
    public CookingRecipes(final long energyPerTick) {
        if (energyPerTick <= 0) {
            throw new IllegalArgumentException("energyPerTick must be positive: " + energyPerTick);
        }
        this.energyPerTick = energyPerTick;
    }

    @Override
    public Optional<MachineRecipe> find(final ServerLevel level, final List<ResourceAmount> available) {
        if (!(available.getFirst().resource() instanceof ItemKey key)) {
            return Optional.empty();
        }
        final SingleRecipeInput input = new SingleRecipeInput(key.toStack(1));
        for (RecipeType<? extends AbstractCookingRecipe> type : TYPES) {
            final Optional<MachineRecipe> found = cook(level, type, input, key);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private <T extends AbstractCookingRecipe> Optional<MachineRecipe> cook(
            final ServerLevel level, final RecipeType<T> type, final SingleRecipeInput input, final ItemKey key) {
        final Optional<RecipeHolder<T>> holder = level.getServer().getRecipeManager().getRecipeFor(type, input, level);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        final AbstractCookingRecipe recipe = holder.get().value();
        final ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(MachineRecipe.of(new ResourceAmount(key, 1),
                new ResourceAmount(ItemKey.of(result), result.getCount()), Math.max(1, recipe.getCookingTime()),
                energyPerTick));
    }
}
