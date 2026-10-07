package com.morphengine.nexus.processing;

import com.geckolib.animation.RawAnimation;
import com.mojang.serialization.Codec;
import com.morphengine.nexus.machine.MachineShape;
import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A kind of machine: what it is called, what recipes it runs, how much FE its work takes, and which animation of its
 * model each phase shows. A new kind is a new constant, with a block for each tier registered for it; the machine core
 * stays the same.
 */
public enum MachineKind implements StringRepresentable {

    /**
     * Smelts, blasts and smokes by the recipes of the game. The heating rods of its model warm up when it starts and
     * cool down when it stops.
     */
    ENERGY_FURNACE("energy_furnace", new CookingRecipes(Balance.FURNACE_ENERGY_PER_TICK),
            Layout.items(Balance.FURNACE_COOLDOWN_TICKS), MachineKind::furnaceAnimation),

    /** Crushes raw ore and ore into dust, two dusts for one ore; its jaws close and grind while it works. */
    CRUSHER("crusher", new ProcessingRecipes<>(NexusRecipes.CRUSHING::get, Balance.CRUSHER_TICKS,
            Balance.CRUSHER_ENERGY_PER_TICK), Layout.items(0), MachineKind::breathingAnimation),

    /** Grinds ingots into dust, one for one; its drum turns slowly at rest and fast at work. */
    PULVERIZER("pulverizer", new ProcessingRecipes<>(NexusRecipes.PULVERIZING::get, Balance.PULVERIZER_TICKS,
            Balance.PULVERIZER_ENERGY_PER_TICK), Layout.items(0), MachineKind::breathingAnimation),

    /** Presses ingots into plates; the head of its piston comes down on the blank. */
    COMPRESSOR("compressor", new ProcessingRecipes<>(NexusRecipes.COMPRESSING::get, Balance.COMPRESSOR_TICKS,
            Balance.COMPRESSOR_ENERGY_PER_TICK), Layout.items(0), MachineKind::breathingAnimation),

    /**
     * Melts ingots and materials into an alloy: three input slots in its only line, which the machine takes from
     * together; the items ride the rails of its model to the mold.
     */
    ALLOY_SMELTER("alloy_smelter", new AlloyRecipes(NexusRecipes.ALLOYING::get, Balance.ALLOY_TICKS,
            Balance.ALLOY_ENERGY_PER_TICK), new Layout(0, new MachineShape(Balance.ALLOY_INPUTS, 1),
            MachineOutput.ITEMS), MachineKind::breathingAnimation),

    /**
     * Presses a fluid out of an item into its tank, which is as big in millibuckets as the buffer of its tier is in
     * FE; it stops when the tank is full. It has one input slot at every tier.
     */
    EXTRACTOR("extractor", new ExtractionRecipes(NexusRecipes.EXTRACTING::get, Balance.EXTRACTOR_TICKS,
            Balance.EXTRACTOR_ENERGY_PER_TICK), new Layout(0, new MachineShape(1, 1), MachineOutput.FLUID),
            MachineKind::breathingAnimation);

    public static final Codec<MachineKind> CODEC = StringRepresentable.fromEnum(MachineKind::values);

    private final String id;
    private final LevelRecipes recipes;
    private final Layout layout;
    private final Map<MachinePhase, RawAnimation> animationOf = new EnumMap<>(MachinePhase.class);

    MachineKind(
            final String id, final LevelRecipes recipes, final Layout layout,
            final Function<MachinePhase, RawAnimation> animations) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.recipes = Objects.requireNonNull(recipes, "recipes must not be null");
        this.layout = Objects.requireNonNull(layout, "layout must not be null");
        for (MachinePhase phase : MachinePhase.values()) {
            animationOf.put(phase, animations.apply(phase));
        }
    }

    private static RawAnimation furnaceAnimation(final MachinePhase phase) {
        return switch (phase) {
            case OFF -> RawAnimation.begin().thenLoop("idle");
            case STANDBY -> RawAnimation.begin().thenLoop("online");
            case ACTIVE -> RawAnimation.begin().thenPlay("heat_up").thenLoop("active");
            case COOLING -> RawAnimation.begin().thenPlay("cool_down").thenLoop("online");
        };
    }

    private static RawAnimation breathingAnimation(final MachinePhase phase) {
        return RawAnimation.begin().thenLoop(switch (phase) {
            case OFF -> "idle";
            case STANDBY, COOLING -> "online";
            case ACTIVE -> "active";
        });
    }

    /**
     * @return the name of the model and of the blocks, which the tier is put in front of
     */
    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public LevelRecipes recipes() {
        return recipes;
    }

    /**
     * @return ticks the machine shows {@link MachinePhase#COOLING} after its work stops, zero for a machine with
     *         nothing that cools
     */
    public int cooldownTicks() {
        return layout.cooldownTicks();
    }

    public MachineShape shape() {
        return layout.shape();
    }

    public MachineOutput output() {
        return layout.output();
    }

    /**
     * @return the color of the model of the machine in an inventory slot, a different one for each kind, where every
     *         machine is a small dark cube; in the hand and in the world it is not colored
     */
    public DyeColor slotColor() {
        return switch (this) {
            case ENERGY_FURNACE -> DyeColor.ORANGE;
            case CRUSHER -> DyeColor.RED;
            case PULVERIZER -> DyeColor.YELLOW;
            case COMPRESSOR -> DyeColor.CYAN;
            case ALLOY_SMELTER -> DyeColor.PURPLE;
            case EXTRACTOR -> DyeColor.LIME;
        };
    }

    /**
     * @return the animation to loop in {@code phase}; asked for on the client every frame, so it allocates nothing
     */
    public RawAnimation animationOf(final MachinePhase phase) {
        return animationOf.get(phase);
    }

    /**
     * How the machine is built.
     *
     * @param cooldownTicks ticks the machine shows {@link MachinePhase#COOLING} after its work stops
     */
    private record Layout(int cooldownTicks, MachineShape shape, MachineOutput output) {

        static Layout items(final int cooldownTicks) {
            return new Layout(cooldownTicks, MachineShape.SINGLE_INPUT, MachineOutput.ITEMS);
        }
    }

    /** Placeholder balance until the numbers are settled. */
    private static final class Balance {

        static final long FURNACE_ENERGY_PER_TICK = 32;
        static final long CRUSHER_ENERGY_PER_TICK = 48;
        static final int CRUSHER_TICKS = 100;
        static final long PULVERIZER_ENERGY_PER_TICK = 40;
        static final int PULVERIZER_TICKS = 80;
        static final long COMPRESSOR_ENERGY_PER_TICK = 64;
        static final int COMPRESSOR_TICKS = 120;
        static final long ALLOY_ENERGY_PER_TICK = 80;
        static final int ALLOY_TICKS = 160;
        static final int ALLOY_INPUTS = 3;
        static final long EXTRACTOR_ENERGY_PER_TICK = 48;
        static final int EXTRACTOR_TICKS = 100;
        /** As long as the cooling animation of the furnace model. */
        static final int FURNACE_COOLDOWN_TICKS = 48;

        private Balance() {
        }
    }
}
