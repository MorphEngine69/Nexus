package com.morphengine.nexus.client.render;

import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.NexusStatus;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Nexus from its GeckoLib model: lit while its network has energy,
 * and in red while it is in conflict.
 */
public final class NexusRenderer extends DeviceRenderer<NexusBlockEntity> {

    public NexusRenderer(final BlockEntityRendererProvider.Context context) {
        super(context, new NexusGeoModel<>(), state -> state.getValue(NexusBlock.STATUS) != NexusStatus.NO_ENERGY);
    }

    @Override
    public void addRenderData(
            final NexusBlockEntity animatable, final @Nullable Void relatedObject,
            final BlockEntityRenderState renderState, final float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);
        NexusRenderData.capture(animatable.getBlockState(), renderState);
    }
}
