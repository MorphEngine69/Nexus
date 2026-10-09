package com.morphengine.nexus.client.render;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.block.NetworkDeviceBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;

import java.util.Locale;

/**
 * The model of a device, for its block. Its texture follows the color of the network, from
 * {@code textures/geo/<asset>/<color>.png}. Designed for extension: a subclass may pick another texture name.
 */
public class DeviceGeoModel<T extends GeoAnimatable> extends AdjustableGeoModel<T> {

    private final String asset;

    /**
     * @param asset the name of the model, its animations and its texture folder
     */
    public DeviceGeoModel(final String asset) {
        this.asset = asset;
    }

    /**
     * @return the name of the model that {@code animatable} is drawn from
     */
    protected String assetOf(final T animatable) {
        return asset;
    }

    @Override
    public ResourceLocation getModelResource(final T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "geo/block/" + assetOf(animatable) + ".geo.json");
    }

    @Override
    public ResourceLocation getAnimationResource(final T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID,
                "animations/block/" + assetOf(animatable) + ".animation.json");
    }

    @Override
    public final ResourceLocation getTextureResource(final T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID,
                "textures/geo/" + assetOf(animatable) + "/" + textureName(animatable) + ".png");
    }

    /**
     * @return the file name, without extension, of the texture to draw now
     */
    protected String textureName(final T animatable) {
        return colorOf(animatable).getName().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the color of the network of a device in the world; the standard color for an item
     */
    protected static DyeColor colorOf(final GeoAnimatable animatable) {
        return animatable instanceof BlockEntity entity && entity.getBlockState().hasProperty(
                NetworkDeviceBlock.NETWORK_COLOR)
                ? entity.getBlockState().getValue(NetworkDeviceBlock.NETWORK_COLOR) : NetworkColoring.UNCONNECTED;
    }
}
