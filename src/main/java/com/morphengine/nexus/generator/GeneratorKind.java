package com.morphengine.nexus.generator;

import com.geckolib.animation.RawAnimation;
import com.mojang.serialization.Codec;
import com.morphengine.nexus.generator.GeneratorFuel.FluidFuel;
import com.morphengine.nexus.generator.GeneratorFuel.ItemFuel;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.registry.NexusFluids;
import com.morphengine.nexus.registry.NexusTags;
import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A kind of generator: what it burns, how much FE a tick of burning makes and how bright its block shines at work.
 * Each is a block of its own with a model of its own; the block entity, the panel and the rules are the same for all.
 * A new kind is a new constant, a block registered for it and the files of its model.
 */
public enum GeneratorKind implements StringRepresentable {

    /** Burns coal, charcoal and coal blocks, as the first generator of the mod did. */
    COAL("coal_generator",
            new ItemFuel(stack -> stack.is(NexusTags.COAL_GENERATOR_FUELS),
                    (level, stack) -> level.fuelValues().burnDuration(stack)),
            GeneratorBalance.COAL_ENERGY_PER_TICK, GeneratorBalance.COAL_LIGHT, true),

    /** Burns nether stars, one for a long time. */
    NETHER_STAR("nether_star_generator",
            new ItemFuel(stack -> stack.is(Items.NETHER_STAR),
                    (level, stack) -> GeneratorBalance.NETHER_STAR_BURN_TICKS),
            GeneratorBalance.NETHER_STAR_ENERGY_PER_TICK, GeneratorBalance.NETHER_STAR_LIGHT, false),

    /** Burns lava from its tank, filled by bucket or by pipe. */
    LAVA("lava_generator",
            new FluidFuel(List.of(new TankSpec(() -> Fluids.LAVA, 1)),
                    GeneratorBalance.LAVA_BURN_TICKS_PER_MILLIBUCKET),
            GeneratorBalance.LAVA_ENERGY_PER_TICK, GeneratorBalance.LAVA_LIGHT, true),

    /** Heats water with lava into steam; needs both tanks. */
    STEAM("steam_generator",
            new FluidFuel(List.of(new TankSpec(() -> Fluids.LAVA, 1),
                    new TankSpec(() -> Fluids.WATER, GeneratorBalance.STEAM_WATER_PER_PORTION)),
                    GeneratorBalance.STEAM_BURN_TICKS_PER_PORTION),
            GeneratorBalance.STEAM_ENERGY_PER_TICK, GeneratorBalance.STEAM_LIGHT, false),

    /** Burns biofuel, the best of them. */
    BIOFUEL("biofuel_generator",
            new FluidFuel(List.of(new TankSpec(() -> NexusFluids.BIOFUEL.get(), 1)),
                    GeneratorBalance.BIOFUEL_BURN_TICKS_PER_MILLIBUCKET),
            GeneratorBalance.BIOFUEL_ENERGY_PER_TICK, GeneratorBalance.BIOFUEL_LIGHT, false);

    public static final Codec<GeneratorKind> CODEC = StringRepresentable.fromEnum(GeneratorKind::values);

    private static final int SLOT_LIMIT = 64;

    private final String id;
    private final GeneratorFuel fuel;
    private final long energyPerTick;
    private final int workLight;
    private final boolean phaseTextures;
    private final Map<MachinePhase, RawAnimation> animationOf = new EnumMap<>(MachinePhase.class);

    GeneratorKind(
            final String id, final GeneratorFuel fuel, final long energyPerTick, final int workLight,
            final boolean phaseTextures) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.fuel = Objects.requireNonNull(fuel, "fuel must not be null");
        this.energyPerTick = energyPerTick;
        this.workLight = workLight;
        this.phaseTextures = phaseTextures;
        for (MachinePhase phase : MachinePhase.values()) {
            animationOf.put(phase, RawAnimation.begin().thenLoop(animationName(phase)));
        }
    }

    private static String animationName(final MachinePhase phase) {
        return switch (phase) {
            case OFF -> "idle";
            case STANDBY, COOLING -> "online";
            case ACTIVE -> "active";
        };
    }

    /**
     * @return the id of the block and of its model, such as {@code coal_generator}
     */
    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public GeneratorFuel fuel() {
        return fuel;
    }

    /**
     * @return whether the stack goes in the input slot: fuel for a generator that burns items, a container of a fluid
     *         for one that burns fluids
     */
    public boolean takesIn(final ItemStack stack) {
        return fuel instanceof ItemFuel item ? item.accepts().test(stack) : BucketSlot.isContainer(stack);
    }

    /**
     * @return what the input slot takes in and gives out
     */
    public GeneratorItems.Rules itemRules() {
        return new GeneratorItems.Rules(resource -> takesIn(resource.toStack(1)),
                resource -> BucketSlot.isSpent(resource.toStack(1)), burnsFluid() ? 1 : SLOT_LIMIT);
    }

    /**
     * @return the fluid each tank takes, in the order of the tanks; none for a generator that burns items
     */
    public List<FluidKey> acceptedFluids() {
        return fuel.tanks().stream().map(spec -> new FluidKey(FluidResource.of(spec.fluid().get()))).toList();
    }

    /**
     * @return FE a tick of burning makes, before Speed Upgrades
     */
    public long energyPerTick() {
        return energyPerTick;
    }

    /**
     * @return the light level of the block while it works
     */
    public int workLight() {
        return workLight;
    }

    /**
     * @return whether the fire or lava of the model is painted on its texture, so that there is a texture for each
     *         phase of the generator
     */
    public boolean hasPhaseTextures() {
        return phaseTextures;
    }

    /**
     * @return whether the generator has tanks and a bucket slot rather than a fuel slot
     */
    public boolean burnsFluid() {
        return fuel instanceof FluidFuel;
    }

    /**
     * @return the animation of the model that the block shows in {@code phase}
     */
    public RawAnimation animationOf(final MachinePhase phase) {
        return animationOf.get(phase);
    }
}
