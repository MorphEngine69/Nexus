package com.morphengine.nexus.client.render;

import com.morphengine.nexus.block.GeneratorBlock;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;

/**
 * The model of a generator, in the color of its network. A generator whose fire or lava is painted on its texture has a
 * texture for each phase; the others have one for each color.
 */
final class GeneratorGeoModel extends KindGeoModel<GeneratorBlockEntity> {

    GeneratorGeoModel(final String defaultAsset) {
        super(generator -> generator.kind().id(), defaultAsset);
    }

    @Override
    protected String textureName(final GeneratorBlockEntity generator) {
        return super.textureName(generator) + GeneratorRenderer.stateTexture(generator.kind(),
                generator.getBlockState().getValue(GeneratorBlock.PHASE));
    }
}
