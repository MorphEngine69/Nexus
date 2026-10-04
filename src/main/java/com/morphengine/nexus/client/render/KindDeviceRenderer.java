package com.morphengine.nexus.client.render;

import com.geckolib.animatable.GeoAnimatable;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Draws a device that comes in several kinds from the model of its kind, which is what a Puller, Pusher, Placer and
 * Remover head and a Terminal, Crafting Terminal and Blueprint Terminal need.
 */
public final class KindDeviceRenderer<T extends BlockEntity & GeoAnimatable> extends DeviceRenderer<T> {

    private final Function<T, String> assetOf;

    /**
     * @param assetOf      the name of the model of a device, which is also the name of its texture folder
     * @param defaultAsset the name of the model drawn when none is known
     * @param isLit        whether the device is lit in a block state
     */
    public KindDeviceRenderer(
            final BlockEntityRendererProvider.Context context, final Function<T, String> assetOf,
            final String defaultAsset, final Predicate<BlockState> isLit) {
        super(context, new KindGeoModel<>(assetOf, defaultAsset), isLit);
        this.assetOf = Objects.requireNonNull(assetOf, "assetOf must not be null");
    }

    @Override
    public void addRenderData(
            final T animatable, final @Nullable Void relatedObject, final BlockEntityRenderState renderState,
            final float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);
        renderState.addGeckolibData(KindGeoModel.ASSET, assetOf.apply(animatable));
    }
}
