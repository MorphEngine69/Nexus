package com.morphengine.nexus.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.NexusStatus;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the Nexus renderer reads besides what every device shows: the status of
 * the Nexus, which also picks its texture. An item shows a running Nexus.
 */
final class NexusRenderData {

    static final DataTicket<NexusStatus> STATUS = DataTicket.create("nexus_status", NexusStatus.class);

    private NexusRenderData() {
    }

    static void capture(final BlockState state, final GeoRenderState renderState) {
        renderState.addGeckolibData(STATUS, state.getValue(NexusBlock.STATUS));
    }

    static NexusStatus statusOf(final GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(STATUS, NexusStatus.ONLINE);
    }
}
