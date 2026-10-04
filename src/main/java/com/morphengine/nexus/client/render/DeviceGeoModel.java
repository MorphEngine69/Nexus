package com.morphengine.nexus.client.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.Nexus;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * The model of a device, for its block and for its item. Its texture follows
 * the color of the network, from {@code textures/geo/<asset>/<color>.png}.
 * Designed for extension: a subclass may pick another texture name.
 */
public class DeviceGeoModel<T extends GeoAnimatable> extends DefaultedBlockGeoModel<T> {

    private final String asset;

    /**
     * @param asset the name of the model, its animations and its texture folder
     */
    public DeviceGeoModel(final String asset) {
        super(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, asset));
        this.asset = asset;
    }

    @Override
    public final Identifier getTextureResource(final GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Nexus.MOD_ID,
                "textures/geo/" + asset + "/" + textureName(renderState) + ".png");
    }

    /**
     * @return the file name, without extension, of the texture to draw now
     */
    protected String textureName(final GeoRenderState renderState) {
        return DeviceRenderData.colorOf(renderState).getName().toLowerCase(Locale.ROOT);
    }
}
