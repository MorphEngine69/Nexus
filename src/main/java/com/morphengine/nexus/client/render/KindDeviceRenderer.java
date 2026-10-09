package com.morphengine.nexus.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoAnimatable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Draws a device that comes in several kinds from the model of its kind, which is what a Puller, Pusher, Placer and
 * Remover head and a Terminal, Crafting Terminal and Blueprint Terminal need.
 */
public final class KindDeviceRenderer<T extends BlockEntity & GeoAnimatable> extends DeviceRenderer<T> {

    /**
     * @param assetOf      the name of the model of a device, which is also the name of its texture folder
     * @param defaultAsset the name of the model drawn when none is known
     * @param isLit        whether the device is lit in a block state
     */
    public KindDeviceRenderer(
            final BlockEntityRendererProvider.Context context, final Function<T, String> assetOf,
            final String defaultAsset, final Predicate<BlockState> isLit) {
        super(context, new KindGeoModel<>(assetOf, defaultAsset), isLit);
    }
}
