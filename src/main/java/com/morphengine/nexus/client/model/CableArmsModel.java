package com.morphengine.nexus.client.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.SideConnections;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The cable a device reaches out with: the cable coupling at the centre and a
 * cable arm on every attached side, so the device reads as the end of a cable
 * run. Each arm takes the color of the cable it meets, and the standard blue
 * where it meets another device; the coupling takes the color of the first
 * attached cable. All of it glows while the block state is {@code powered}.
 * The parts are the cable's own models, baked once for every color, glow and
 * side; the choice is made per block from its neighbours when its section is
 * built. Client side only.
 */
public final class CableArmsModel implements DynamicBlockStateModel {

    /** The model type named in a block state file. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "cable_arms");

    private static final int SIDES = Direction.values().length;
    private static final int GLOWS = 2;
    private static final int BITS_PER_SIDE = 5;
    private static final int COLOR_MASK = 0xF;

    private final BlockStateModelPart[] arms;
    private final BlockStateModelPart[] couplings;

    private CableArmsModel(final BlockStateModelPart[] arms, final BlockStateModelPart[] couplings) {
        this.arms = arms;
        this.couplings = couplings;
    }

    @Override
    public void collectParts(
            final BlockAndTintGetter level, final BlockPos pos, final BlockState state, final RandomSource random,
            final List<BlockStateModelPart> output) {
        final int glow = glowOf(state);
        DyeColor coupling = null;
        for (Direction side : Direction.values()) {
            if (SideConnections.isAttached(state, side)) {
                final DyeColor color = colorTowards(level, pos, side);
                output.add(arms[(color.ordinal() * GLOWS + glow) * SIDES + side.ordinal()]);
                if (coupling == null) {
                    coupling = color;
                }
            }
        }
        output.add(couplings[(coupling != null ? coupling : CableBlock.DEFAULT_COLOR).ordinal() * GLOWS + glow]);
    }

    /**
     * Without a level, such as for breaking particles, the plain coupling.
     */
    @Override
    public void collectParts(final RandomSource random, final List<BlockStateModelPart> output) {
        output.add(couplings[CableBlock.DEFAULT_COLOR.ordinal() * GLOWS]);
    }

    /**
     * @return what decides the parts: the glow and, for each side, whether an
     *         arm is there and its color
     */
    @Override
    public Object createGeometryKey(
            final BlockAndTintGetter level, final BlockPos pos, final BlockState state, final RandomSource random) {
        long key = glowOf(state);
        for (Direction side : Direction.values()) {
            final long arm = SideConnections.isAttached(state, side)
                    ? 1 << (BITS_PER_SIDE - 1) | colorTowards(level, pos, side).ordinal() & COLOR_MASK : 0;
            key |= arm << (1 + side.ordinal() * BITS_PER_SIDE);
        }
        return new GeometryKey(this, key);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Material.Baked particleMaterial() {
        return couplings[CableBlock.DEFAULT_COLOR.ordinal() * GLOWS].particleMaterial();
    }

    @Override
    @SuppressWarnings("deprecation")
    public @BakedQuad.MaterialFlags int materialFlags() {
        return 0;
    }

    private static int glowOf(final BlockState state) {
        return state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED)
                ? 1 : 0;
    }

    private static DyeColor colorTowards(final BlockAndTintGetter level, final BlockPos pos, final Direction side) {
        return level.getBlockState(pos.relative(side)).getBlock() instanceof CableBlock cable
                ? cable.color() : CableBlock.DEFAULT_COLOR;
    }

    private record GeometryKey(CableArmsModel model, long parts) {
    }

    /**
     * The model as a block state file names it: {@code {"type": "nexus:cable_arms"}}.
     */
    public static final class Unbaked implements CustomUnbakedBlockStateModel {

        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        /** How a model facing north is turned to face each side, as in the block state files. */
        private static final Map<Direction, ModelState> TURNS = turns();

        private Unbaked() {
        }

        @Override
        public MapCodec<Unbaked> codec() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(final ResolvableModel.Resolver resolver) {
            for (DyeColor color : DyeColor.values()) {
                for (int glow = 0; glow < GLOWS; glow++) {
                    resolver.markDependency(part(color, "arm", glow));
                    resolver.markDependency(part(color, "coupling", glow));
                }
            }
        }

        @Override
        public BlockStateModel bake(final ModelBaker baker) {
            final BlockStateModelPart[] arms = new BlockStateModelPart[DyeColor.values().length * GLOWS * SIDES];
            final BlockStateModelPart[] couplings = new BlockStateModelPart[DyeColor.values().length * GLOWS];
            for (DyeColor color : DyeColor.values()) {
                for (int glow = 0; glow < GLOWS; glow++) {
                    final int index = color.ordinal() * GLOWS + glow;
                    couplings[index] = SimpleModelWrapper.bake(baker, part(color, "coupling", glow),
                            TURNS.get(Direction.NORTH));
                    for (Direction side : Direction.values()) {
                        arms[index * SIDES + side.ordinal()] =
                                SimpleModelWrapper.bake(baker, part(color, "arm", glow), TURNS.get(side));
                    }
                }
            }
            return new CableArmsModel(arms, couplings);
        }

        private static Identifier part(final DyeColor color, final String name, final int glow) {
            return Identifier.fromNamespaceAndPath(Nexus.MOD_ID,
                    "block/cable/" + color.getSerializedName() + "/" + name + (glow == 1 ? "_powered" : ""));
        }

        private static Map<Direction, ModelState> turns() {
            final Map<Direction, ModelState> turns = new EnumMap<>(Direction.class);
            turns.put(Direction.NORTH, turn(Quadrant.R0, Quadrant.R0));
            turns.put(Direction.EAST, turn(Quadrant.R0, Quadrant.R90));
            turns.put(Direction.SOUTH, turn(Quadrant.R0, Quadrant.R180));
            turns.put(Direction.WEST, turn(Quadrant.R0, Quadrant.R270));
            turns.put(Direction.UP, turn(Quadrant.R270, Quadrant.R0));
            turns.put(Direction.DOWN, turn(Quadrant.R90, Quadrant.R0));
            return turns;
        }

        private static ModelState turn(final Quadrant x, final Quadrant y) {
            return new Variant.SimpleModelState(x, y, Quadrant.R0, false).asModelState();
        }
    }
}
