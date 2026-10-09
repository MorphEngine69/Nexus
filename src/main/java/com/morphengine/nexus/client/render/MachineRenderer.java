package com.morphengine.nexus.client.render;

import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachineMarks;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import java.util.List;

/**
 * Draws a machine from the model of its kind, in the color of its network, with a port on every side where a cable is
 * attached and the front free of one, and with the marks of its own tier only.
 */
public final class MachineRenderer extends FacedDeviceRenderer<MachineBlockEntity> {

    public MachineRenderer(final BlockEntityRendererProvider.Context context) {
        super(context, new KindGeoModel<>(machine -> machine.kind().id(), MachineKind.ENERGY_FURNACE.id()),
                state -> state.getValue(MachineBlock.PHASE).isLit(), MachineBlock.FACING, List.of(), List.of());
        addRenderLayer(new MachineItemsLayer(this, context.getItemRenderer()));
    }

    @Override
    protected void adjustDeviceBones(
            final MachineBlockEntity machine, final Bones bones, final int ports, final float partialTick) {
        for (String other : MachineMarks.bonesOfOtherRanks(machine.machine().tier().rank())) {
            bones.hide(other, true);
        }
    }
}
