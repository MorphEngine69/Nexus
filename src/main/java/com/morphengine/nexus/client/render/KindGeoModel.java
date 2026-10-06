package com.morphengine.nexus.client.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.Nexus;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.Function;

/**
 * The model of a device that comes in several kinds, one block entity type for all of them: its model, animations and
 * texture, which follows the color of the network, are those of the kind it is.
 */
class KindGeoModel<T extends GeoAnimatable> extends GeoModel<T> {

    static final DataTicket<String> ASSET = DataTicket.create("kind_asset", String.class);

    private final Function<T, String> assetOf;
    private final String defaultAsset;

    /**
     * @param assetOf      the name of the model of a device
     * @param defaultAsset the name of the model drawn when the render state does not tell
     */
    KindGeoModel(final Function<T, String> assetOf, final String defaultAsset) {
        this.assetOf = Objects.requireNonNull(assetOf, "assetOf must not be null");
        this.defaultAsset = Objects.requireNonNull(defaultAsset, "defaultAsset must not be null");
    }

    @Override
    public Identifier getModelResource(final GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "block/" + assetIn(renderState));
    }

    @Override
    public final Identifier getTextureResource(final GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "textures/geo/" + assetIn(renderState)
                + "/" + textureName(renderState) + ".png");
    }

    /**
     * @return the file name, without extension, of the texture to draw now: the color of the network, unless a
     *         subclass adds to it
     */
    protected String textureName(final GeoRenderState renderState) {
        return DeviceRenderData.colorOf(renderState).getName();
    }

    @Override
    public Identifier getAnimationResource(final T animatable) {
        return Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "block/" + assetOf.apply(animatable));
    }

    private String assetIn(final GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(ASSET, defaultAsset);
    }
}
