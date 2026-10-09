package com.morphengine.nexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Lights the accents of a model while it is lit: draws the model again with the {@code _glowmask} texture beside its
 * own, full bright; dark otherwise, when the base texture's muted accents are all that shows. The glow mask is a
 * texture of its own, not a mask over the base one as in GeckoLib's own glow layer, so that the lit accents can be
 * brighter than the muted ones.
 */
final class GlowWhenLitLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    private static final String GLOW_SUFFIX = "_glowmask";
    private static final String EXTENSION = ".png";

    private final Predicate<T> isLit;

    GlowWhenLitLayer(final GeoRenderer<T> renderer, final Predicate<T> isLit) {
        super(renderer);
        this.isLit = Objects.requireNonNull(isLit, "isLit must not be null");
    }

    @Override
    public void render(
            final PoseStack poseStack, final T animatable, final BakedGeoModel bakedModel,
            final @Nullable RenderType renderType, final MultiBufferSource bufferSource,
            final @Nullable VertexConsumer buffer, final float partialTick, final int packedLight,
            final int packedOverlay) {
        if (!isLit.test(animatable)) {
            return;
        }
        final RenderType glowing = RenderType.eyes(glowTextureOf(getTextureResource(animatable)));
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowing,
                bufferSource.getBuffer(glowing), partialTick, LightTexture.FULL_SKY, packedOverlay,
                getRenderer().getRenderColor(animatable, partialTick, packedLight).argbInt());
    }

    private static ResourceLocation glowTextureOf(final ResourceLocation base) {
        final String path = base.getPath();
        final String stem = path.endsWith(EXTENSION) ? path.substring(0, path.length() - EXTENSION.length()) : path;
        return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), stem + GLOW_SUFFIX + EXTENSION);
    }
}
