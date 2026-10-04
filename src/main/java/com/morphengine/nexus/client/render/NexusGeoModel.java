package com.morphengine.nexus.client.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.block.NexusStatus;

/**
 * The Nexus model: a Nexus in conflict wears a texture of its own.
 */
public final class NexusGeoModel<T extends GeoAnimatable> extends DeviceGeoModel<T> {

    private static final String CONFLICT_TEXTURE = "conflict";

    public NexusGeoModel() {
        super("nexus");
    }

    @Override
    protected String textureName(final GeoRenderState renderState) {
        return NexusRenderData.statusOf(renderState) == NexusStatus.CONFLICT
                ? CONFLICT_TEXTURE : super.textureName(renderState);
    }
}
