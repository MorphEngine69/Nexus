package com.morphengine.nexus.client.render;

import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.NexusStatus;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;

/**
 * The Nexus model: a Nexus in conflict wears a texture of its own.
 */
public final class NexusGeoModel<T extends GeoAnimatable> extends DeviceGeoModel<T> {

    private static final String CONFLICT_TEXTURE = "conflict";

    public NexusGeoModel() {
        super("nexus");
    }

    @Override
    protected String textureName(final T animatable) {
        final boolean inConflict = animatable instanceof BlockEntity entity
                && entity.getBlockState().hasProperty(NexusBlock.STATUS)
                && entity.getBlockState().getValue(NexusBlock.STATUS) == NexusStatus.CONFLICT;
        return inConflict ? CONFLICT_TEXTURE : super.textureName(animatable);
    }
}
