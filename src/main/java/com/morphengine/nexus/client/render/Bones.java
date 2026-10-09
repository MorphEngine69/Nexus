package com.morphengine.nexus.client.render;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The bones of the model that is about to be drawn. A model without the bone of a name simply has no such part, so
 * every change to a bone that is not there is left out.
 */
final class Bones {

    private final GeoModel<?> model;

    Bones(final GeoModel<?> model) {
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    void ifPresent(final String name, final Consumer<GeoBone> change) {
        model.getBone(name).ifPresent(change);
    }

    /**
     * Hides or shows the cubes of the bone; the bones inside it are not touched.
     */
    void hide(final String name, final boolean hidden) {
        ifPresent(name, bone -> bone.setHidden(hidden));
    }

    void scale(final String name, final float x, final float y, final float z) {
        ifPresent(name, bone -> bone.updateScale(x, y, z));
    }
}
