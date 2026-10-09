package com.morphengine.nexus.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.SideConnections;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
public final class CableArmsModel implements BakedModel {

    /** The loader named in a model file: {@code {"loader": "nexus:cable_arms"}}. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "cable_arms");

    /** What the parts of a block are, found from its neighbours when its section is built. */
    private static final ModelProperty<List<BakedModel>> PARTS = new ModelProperty<>();

    private static final int SIDES = Direction.values().length;
    private static final int GLOWS = 2;

    private final BakedModel[] arms;
    private final BakedModel[] couplings;

    private CableArmsModel(final BakedModel[] arms, final BakedModel[] couplings) {
        this.arms = arms;
        this.couplings = couplings;
    }

    @Override
    public ModelData getModelData(
            final BlockAndTintGetter level, final BlockPos pos, final BlockState state, final ModelData data) {
        final int glow = glowOf(state);
        final List<BakedModel> parts = new ArrayList<>(SIDES + 1);
        DyeColor coupling = null;
        for (Direction side : Direction.values()) {
            if (SideConnections.isAttached(state, side)) {
                final DyeColor color = colorTowards(level, pos, side);
                parts.add(arms[(color.ordinal() * GLOWS + glow) * SIDES + side.ordinal()]);
                if (coupling == null) {
                    coupling = color;
                }
            }
        }
        parts.add(couplings[(coupling != null ? coupling : CableBlock.DEFAULT_COLOR).ordinal() * GLOWS + glow]);
        return data.derive().with(PARTS, List.copyOf(parts)).build();
    }

    @Override
    public List<BakedQuad> getQuads(
            final @Nullable BlockState state, final @Nullable Direction side, final RandomSource random,
            final ModelData data, final @Nullable RenderType renderType) {
        final List<BakedModel> parts = data.get(PARTS);
        if (parts == null) {
            return couplings[CableBlock.DEFAULT_COLOR.ordinal() * GLOWS].getQuads(state, side, random,
                    ModelData.EMPTY, renderType);
        }
        final List<BakedQuad> quads = new ArrayList<>();
        for (BakedModel part : parts) {
            quads.addAll(part.getQuads(state, side, random, ModelData.EMPTY, renderType));
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(
            final @Nullable BlockState state, final @Nullable Direction side, final RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return couplings[CableBlock.DEFAULT_COLOR.ordinal() * GLOWS].getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    private static int glowOf(final BlockState state) {
        return state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED)
                ? 1 : 0;
    }

    private static DyeColor colorTowards(final BlockAndTintGetter level, final BlockPos pos, final Direction side) {
        return level.getBlockState(pos.relative(side)).getBlock() instanceof CableBlock cable
                ? cable.color() : CableBlock.DEFAULT_COLOR;
    }

    /**
     * The loader of the model, registered in the model event.
     */
    public static final class Loader implements IGeometryLoader<Geometry> {

        public static final Loader INSTANCE = new Loader();

        private Loader() {
        }

        @Override
        public Geometry read(final JsonObject json, final JsonDeserializationContext context) {
            return new Geometry();
        }
    }

    /**
     * The model as a model file names it.
     */
    public static final class Geometry implements IUnbakedGeometry<Geometry> {

        /** How a model facing north is turned to face each side, as in the block state files. */
        private static final int QUARTER_TURN = 90;
        private static final int HALF_TURN = 180;
        private static final int THREE_QUARTER_TURN = 270;
        private static final Map<Direction, ModelState> TURNS = turns();

        private Geometry() {
        }

        @Override
        public void resolveParents(
                final Function<ResourceLocation, UnbakedModel> modelGetter, final IGeometryBakingContext context) {
            for (DyeColor color : DyeColor.values()) {
                for (int glow = 0; glow < GLOWS; glow++) {
                    modelGetter.apply(part(color, "arm", glow)).resolveParents(modelGetter);
                    modelGetter.apply(part(color, "coupling", glow)).resolveParents(modelGetter);
                }
            }
        }

        @Override
        public BakedModel bake(
                final IGeometryBakingContext context, final ModelBaker baker,
                final Function<Material, TextureAtlasSprite> spriteGetter, final ModelState modelState,
                final ItemOverrides overrides) {
            final BakedModel[] arms = new BakedModel[DyeColor.values().length * GLOWS * SIDES];
            final BakedModel[] couplings = new BakedModel[DyeColor.values().length * GLOWS];
            for (DyeColor color : DyeColor.values()) {
                for (int glow = 0; glow < GLOWS; glow++) {
                    final int index = color.ordinal() * GLOWS + glow;
                    couplings[index] = baker.bake(part(color, "coupling", glow), TURNS.get(Direction.NORTH),
                            spriteGetter);
                    for (Direction side : Direction.values()) {
                        arms[index * SIDES + side.ordinal()] =
                                baker.bake(part(color, "arm", glow), TURNS.get(side), spriteGetter);
                    }
                }
            }
            return new CableArmsModel(arms, couplings);
        }

        private static ResourceLocation part(final DyeColor color, final String name, final int glow) {
            return ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID,
                    "block/cable/" + color.getSerializedName() + "/" + name + (glow == 1 ? "_powered" : ""));
        }

        private static Map<Direction, ModelState> turns() {
            final Map<Direction, ModelState> turns = new EnumMap<>(Direction.class);
            turns.put(Direction.NORTH, BlockModelRotation.by(0, 0));
            turns.put(Direction.EAST, BlockModelRotation.by(0, QUARTER_TURN));
            turns.put(Direction.SOUTH, BlockModelRotation.by(0, HALF_TURN));
            turns.put(Direction.WEST, BlockModelRotation.by(0, THREE_QUARTER_TURN));
            turns.put(Direction.UP, BlockModelRotation.by(THREE_QUARTER_TURN, 0));
            turns.put(Direction.DOWN, BlockModelRotation.by(QUARTER_TURN, 0));
            return turns;
        }
    }
}
