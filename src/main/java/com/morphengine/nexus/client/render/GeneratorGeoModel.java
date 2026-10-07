package com.morphengine.nexus.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;

/**
 * The model of a generator, in the color of its network. A generator whose fire or lava is painted on its texture has a
 * texture for each phase, which the renderer names with {@link #STATE}; the others have one for each color.
 */
final class GeneratorGeoModel extends KindGeoModel<GeneratorBlockEntity> {

    /** What follows the color in the name of the texture, such as {@code _active}; nothing for a still texture. */
    static final DataTicket<String> STATE = DataTicket.create("generator_state_texture", String.class);

    GeneratorGeoModel(final String defaultAsset) {
        super(generator -> generator.kind().id(), defaultAsset);
    }

    @Override
    protected String textureName(final GeoRenderState renderState) {
        return super.textureName(renderState) + renderState.getOrDefaultGeckolibData(STATE, "");
    }
}
