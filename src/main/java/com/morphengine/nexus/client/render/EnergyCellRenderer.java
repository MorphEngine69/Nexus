package com.morphengine.nexus.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Draws an Energy Cell: its battery on each side shows the charge, and while the cell charges its segments appear
 * from the left up to the charge. A port on a side takes the place of the battery there.
 */
public final class EnergyCellRenderer extends DeviceRenderer<EnergyCellBlockEntity> {

    private static final DataTicket<Integer> CHARGE = DataTicket.create("cell_charge", Integer.class);
    private static final DataTicket<Boolean> CHARGING = DataTicket.create("cell_charging", Boolean.class);
    private static final DataTicket<Float> TICKS = DataTicket.create("cell_ticks", Float.class);
    private static final List<String> BONES_UNDER_PORTS = List.of("battery");

    public EnergyCellRenderer(final BlockEntityRendererProvider.Context context) {
        super(context, new DeviceGeoModel<>("energy_cell"),
                state -> state.getValue(EnergyCellBlock.CHARGE) > 0);
    }

    @Override
    public void addRenderData(
            final EnergyCellBlockEntity animatable, final @Nullable Void relatedObject,
            final BlockEntityRenderState renderState, final float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);
        renderState.addGeckolibData(CHARGE, animatable.getBlockState().getValue(EnergyCellBlock.CHARGE));
        renderState.addGeckolibData(CHARGING, animatable.getBlockState().getValue(EnergyCellBlock.CHARGING));
        renderState.addGeckolibData(TICKS, animatable.getLevel() == null
                ? 0F : animatable.getLevel().getGameTime() + partialTick);
    }

    @Override
    protected List<String> bonesUnderPorts() {
        return BONES_UNDER_PORTS;
    }

    @Override
    protected void adjustDeviceBones(final GeoRenderState renderState, final BoneSnapshots snapshots) {
        final int level = renderState.getOrDefaultGeckolibData(CHARGE, 0);
        final boolean charging = renderState.getOrDefaultGeckolibData(CHARGING, false);
        final float ticks = renderState.getOrDefaultGeckolibData(TICKS, 0F);
        final int ports = DeviceRenderData.portsOf(renderState);
        for (Direction side : ChargeBar.sides()) {
            final boolean covered = (ports & 1 << side.ordinal()) != 0;
            for (int number = 1; number <= EnergyCellBlock.SEGMENTS; number++) {
                final float fill = covered ? 0 : ChargeBar.fill(number - 1, level, charging, ticks);
                final boolean alongZ = ChargeBar.runsAlongZ(side);
                snapshots.ifPresent(ChargeBar.boneName(number, side), bone -> {
                    bone.skipRender(fill <= 0);
                    bone.setScale(alongZ ? 1 : fill, 1, alongZ ? fill : 1);
                });
            }
        }
    }
}
