package com.morphengine.nexus.client.render;

import software.bernie.geckolib.animatable.GeoAnimatable;

import java.util.Objects;
import java.util.function.Function;

/**
 * The model of a device that comes in several kinds, one block entity type for all of them: its model, animations and
 * texture, which follows the color of the network, are those of the kind it is.
 */
class KindGeoModel<T extends GeoAnimatable> extends DeviceGeoModel<T> {

    private final Function<T, String> assetOf;

    /**
     * @param assetOf      the name of the model of a device
     * @param defaultAsset the name of the model drawn when nothing else tells
     */
    KindGeoModel(final Function<T, String> assetOf, final String defaultAsset) {
        super(Objects.requireNonNull(defaultAsset, "defaultAsset must not be null"));
        this.assetOf = Objects.requireNonNull(assetOf, "assetOf must not be null");
    }

    @Override
    protected String assetOf(final T animatable) {
        return assetOf.apply(animatable);
    }
}
