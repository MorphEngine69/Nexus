package com.morphengine.nexus.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachineMarks;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Draws a machine from the model of its kind, in the color of its network, with a port on every side where a cable is
 * attached and the front free of one, and with the marks of its own tier only.
 */
public final class MachineRenderer extends FacedDeviceRenderer<MachineBlockEntity> {

    private static final DataTicket<Integer> RANK = DataTicket.create("machine_rank", Integer.class);

    public MachineRenderer(final BlockEntityRendererProvider.Context context) {
        super(context, new KindGeoModel<>(machine -> machine.kind().id(), MachineKind.ENERGY_FURNACE.id()),
                state -> state.getValue(MachineBlock.PHASE).isLit(), MachineBlock.FACING, List.of(), List.of());
        withRenderLayer(new MachineItemsLayer(this, context.itemModelResolver()));
    }

    @Override
    public void addRenderData(
            final MachineBlockEntity animatable, final @Nullable Void relatedObject,
            final BlockEntityRenderState renderState, final float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);
        renderState.addGeckolibData(KindGeoModel.ASSET, animatable.kind().id());
        renderState.addGeckolibData(RANK, animatable.machine().tier().rank());
    }

    @Override
    protected void adjustDeviceBones(final GeoRenderState renderState, final BoneSnapshots snapshots) {
        for (String other : MachineMarks.bonesOfOtherRanks(renderState.getOrDefaultGeckolibData(RANK, 1))) {
            snapshots.ifPresent(other, bone -> bone.skipRender(true));
        }
    }
}
