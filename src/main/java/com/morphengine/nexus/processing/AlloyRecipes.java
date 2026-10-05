package com.morphengine.nexus.processing;

import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The alloying recipes, for a machine that has a time and a price in FE of its own: the same for every recipe.
 */
public final class AlloyRecipes implements LevelRecipes {

    private final Supplier<RecipeType<AlloyingRecipe>> type;
    private final int ticks;
    private final long energyPerTick;

    /**
     * @param type          the recipe type, asked for when a level is there, as registries are not filled before
     * @param ticks         ticks the work takes at normal speed, positive
     * @param energyPerTick FE the work takes in each tick at normal speed, positive
     */
    public AlloyRecipes(final Supplier<RecipeType<AlloyingRecipe>> type, final int ticks, final long energyPerTick) {
        this.type = type;
        this.ticks = ticks;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public Optional<MachineRecipe> find(final ServerLevel level, final List<ResourceAmount> available) {
        final List<ItemStack> stacks = new ArrayList<>(available.size());
        for (ResourceAmount held : available) {
            if (held.resource() instanceof ItemKey key) {
                stacks.add(key.toStack((int) Math.min(held.amount(), Integer.MAX_VALUE)));
            }
        }
        final AlloyInput input = new AlloyInput(stacks);
        final Optional<RecipeHolder<AlloyingRecipe>> holder = level.getServer().getRecipeManager()
                .getRecipeFor(type.get(), input, level);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        final AlloyingRecipe recipe = holder.get().value();
        final List<ResourceAmount> inputs = new ArrayList<>();
        for (ItemStack used : recipe.pick(input)) {
            inputs.add(new ResourceAmount(ItemKey.of(used), used.getCount()));
        }
        final ItemStack result = recipe.assemble(input);
        return result.isEmpty() ? Optional.empty() : Optional.of(new MachineRecipe(inputs,
                new ResourceAmount(ItemKey.of(result), result.getCount()), ticks, energyPerTick));
    }

    @Override
    public boolean usesItem(final ServerLevel level, final ItemKey key) {
        final ItemStack stack = key.toStack(1);
        return level.getServer().getRecipeManager().recipeMap().byType(type.get()).stream()
                .anyMatch(holder -> holder.value().usesItem(stack));
    }
}
