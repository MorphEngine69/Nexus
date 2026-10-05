package com.morphengine.nexus.processing;

import com.morphengine.nexus.api.machine.MachineRecipe;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The extracting recipes, which turn one item into a fluid, for a machine that has a time and a price in FE of its own.
 */
public final class ExtractionRecipes implements LevelRecipes {

    private final Supplier<RecipeType<ExtractingRecipe>> type;
    private final int ticks;
    private final long energyPerTick;

    /**
     * @param type          the recipe type, asked for when a level is there, as registries are not filled before
     * @param ticks         ticks the work takes at normal speed, positive
     * @param energyPerTick FE the work takes in each tick at normal speed, positive
     */
    public ExtractionRecipes(
            final Supplier<RecipeType<ExtractingRecipe>> type, final int ticks, final long energyPerTick) {
        this.type = type;
        this.ticks = ticks;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public Optional<MachineRecipe> find(final ServerLevel level, final List<ResourceAmount> available) {
        if (!(available.getFirst().resource() instanceof ItemKey key)) {
            return Optional.empty();
        }
        final Optional<RecipeHolder<ExtractingRecipe>> holder = level.getServer().getRecipeManager()
                .getRecipeFor(type.get(), new SingleRecipeInput(key.toStack(1)), level);
        return holder.map(found -> MachineRecipe.of(new ResourceAmount(key, 1),
                new ResourceAmount(new FluidKey(FluidResource.of(found.value().fluid())), found.value().amount()),
                ticks, energyPerTick));
    }
}
