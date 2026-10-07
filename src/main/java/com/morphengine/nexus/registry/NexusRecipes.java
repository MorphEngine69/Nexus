package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.processing.AlloyingRecipe;
import com.morphengine.nexus.processing.CompressingRecipe;
import com.morphengine.nexus.processing.CrushingRecipe;
import com.morphengine.nexus.processing.ExtractingRecipe;
import com.morphengine.nexus.processing.PulverizingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The types and serializers of the recipes of the machines.
 */
public final class NexusRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Nexus.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Nexus.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING = type("crushing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<PulverizingRecipe>> PULVERIZING =
            type("pulverizing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CompressingRecipe>> COMPRESSING =
            type("compressing");

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyingRecipe>> ALLOYING = type("alloying");
    public static final DeferredHolder<RecipeType<?>, RecipeType<ExtractingRecipe>> EXTRACTING = type("extracting");

    static {
        SERIALIZERS.register("alloying", () -> AlloyingRecipe.SERIALIZER);
        SERIALIZERS.register("extracting", () -> ExtractingRecipe.SERIALIZER);
        SERIALIZERS.register("crushing", () -> CrushingRecipe.SERIALIZER);
        SERIALIZERS.register("pulverizing", () -> PulverizingRecipe.SERIALIZER);
        SERIALIZERS.register("compressing", () -> CompressingRecipe.SERIALIZER);
    }

    private NexusRecipes() {
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>>
            DeferredHolder<RecipeType<?>, RecipeType<T>> type(final String name) {
        return TYPES.register(name, () -> RecipeType.simple(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name)));
    }
}
