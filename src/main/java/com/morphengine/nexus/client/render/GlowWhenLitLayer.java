package com.morphengine.nexus.client.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

/**
 * Lights the accents of a device in the network color while the device is lit;
 * dark otherwise, when the base texture's muted accents are all that shows.
 */
final class GlowWhenLitLayer<T extends GeoAnimatable, O, R extends GeoRenderState>
        extends AutoGlowingGeoLayer<T, O, R> {

    GlowWhenLitLayer(final GeoRenderer<T, O, R> renderer) {
        super(renderer);
    }

    @Override
    protected @Nullable RenderType getRenderType(final R renderState) {
        return DeviceRenderData.isLit(renderState) ? super.getRenderType(renderState) : null;
    }
}
