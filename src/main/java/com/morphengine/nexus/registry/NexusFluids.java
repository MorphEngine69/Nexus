package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.NexusFluidBlock;
import com.morphengine.nexus.block.NexusFluidType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The fluids of the mod: today only Biofuel, the fuel that the Extractor presses out of plants.
 */
public final class NexusFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Nexus.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Nexus.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> BIOFUEL_TYPE = FLUID_TYPES.register(
            "biofuel", () -> new NexusFluidType(biofuelTypeProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BIOFUEL =
            FLUIDS.register("biofuel", () -> new BaseFlowingFluid.Source(biofuelProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> BIOFUEL_FLOWING =
            FLUIDS.register("flowing_biofuel", () -> new BaseFlowingFluid.Flowing(biofuelProperties()));

    public static final DeferredBlock<NexusFluidBlock> BIOFUEL_BLOCK = NexusBlocks.BLOCKS.registerBlock(
            "biofuel", properties -> new NexusFluidBlock(BIOFUEL.get(), properties), NexusFluids::liquid);

    public static final DeferredItem<BucketItem> BIOFUEL_BUCKET = NexusItems.ITEMS.registerItem(
            "biofuel_bucket",
            properties -> new BucketItem(BIOFUEL.get(), properties.craftRemainder(Items.BUCKET).stacksTo(1)));

    private static final String BIOFUEL_DESCRIPTION_ID = "fluid_type.nexus.biofuel";
    private static final int BIOFUEL_DENSITY = 900;
    private static final int BIOFUEL_VISCOSITY = 1200;
    private static final float LIQUID_HARDNESS = 100.0F;
    /** Spreads as lava does in the Overworld: slowly, and not far. */
    private static final int LAVA_SLOPE_DISTANCE = 2;
    private static final int LAVA_LEVEL_DECREASE = 2;
    private static final int LAVA_TICK_RATE = 30;

    private NexusFluids() {
    }

    /**
     * Loads the class, which registers the fluids; called once from the constructor of the mod.
     */
    public static void bootstrap() {
        // Nothing to do: the registrations are in the static fields.
    }

    private static FluidType.Properties biofuelTypeProperties() {
        return FluidType.Properties.create().descriptionId(BIOFUEL_DESCRIPTION_ID).density(BIOFUEL_DENSITY)
                .viscosity(BIOFUEL_VISCOSITY).canDrown(false);
    }

    private static BaseFlowingFluid.Properties biofuelProperties() {
        return new BaseFlowingFluid.Properties(BIOFUEL_TYPE, BIOFUEL, BIOFUEL_FLOWING)
                .bucket(BIOFUEL_BUCKET).block(BIOFUEL_BLOCK).slopeFindDistance(LAVA_SLOPE_DISTANCE)
                .levelDecreasePerBlock(LAVA_LEVEL_DECREASE).tickRate(LAVA_TICK_RATE);
    }

    private static BlockBehaviour.Properties liquid(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.COLOR_GREEN).replaceable().noCollision().strength(LIQUID_HARDNESS)
                .pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY);
    }
}
