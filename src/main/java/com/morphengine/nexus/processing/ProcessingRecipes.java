package com.morphengine.nexus.processing;

import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The recipes of one type that turn one item into another, for a machine that has a time and a price in FE of its own:
 * the same for every recipe of the type.
 *
 * @param <T> the recipe type
 */
public final class ProcessingRecipes<T extends SingleItemRecipe> implements LevelRecipes {

    private final Supplier<RecipeType<T>> type;
    private final int ticks;
    private final long energyPerTick;

    /**
     * @param type          the recipe type, asked for when a level is there, as registries are not filled before
     * @param ticks         ticks the work of one recipe takes at normal speed, positive
     * @param energyPerTick FE the work takes in each tick at normal speed, positive
     */
    public ProcessingRecipes(final Supplier<RecipeType<T>> type, final int ticks, final long energyPerTick) {
        if (ticks <= 0 || energyPerTick <= 0) {
            throw new IllegalArgumentException("ticks and energyPerTick must be positive: " + ticks + ", "
                    + energyPerTick);
        }
        this.type = type;
        this.ticks = ticks;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public Optional<MachineRecipe> find(final ServerLevel level, final List<ResourceAmount> available) {
        if (!(available.getFirst().resource() instanceof ItemKey key)) {
            return Optional.empty();
        }
        final SingleRecipeInput input = new SingleRecipeInput(key.toStack(1));
        final Optional<RecipeHolder<T>> holder = level.getServer().getRecipeManager()
                .getRecipeFor(type.get(), input, level);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        final ItemStack result = holder.get().value().assemble(input);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(MachineRecipe.of(new ResourceAmount(key, 1),
                new ResourceAmount(ItemKey.of(result), result.getCount()), ticks, energyPerTick));
    }
}
