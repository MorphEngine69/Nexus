package com.morphengine.nexus.client.render;

import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellMarks;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws an Energy Cell: its battery on each side shows the charge, and while the cell charges its segments appear
 * from the left up to the charge. A port on a side takes the place of the battery there.
 */
public final class EnergyCellRenderer extends DeviceRenderer<EnergyCellBlockEntity> {

    private static final List<String> BONES_UNDER_PORTS = underPorts();

    public EnergyCellRenderer(final BlockEntityRendererProvider.Context context) {
        super(context, new DeviceGeoModel<>("energy_cell"),
                state -> state.getValue(EnergyCellBlock.CHARGE) > 0);
    }

    private static List<String> underPorts() {
        final List<String> prefixes = new ArrayList<>(List.of("battery"));
        for (int rank = 1; rank <= EnergyCellMarks.MAX_RANK; rank++) {
            prefixes.add(EnergyCellMarks.bonePrefix(rank));
        }
        return List.copyOf(prefixes);
    }

    @Override
    protected List<String> bonesUnderPorts() {
        return BONES_UNDER_PORTS;
    }

    @Override
    protected void adjustDeviceBones(
            final EnergyCellBlockEntity cell, final Bones bones, final int ports, final float partialTick) {
        final int level = cell.getBlockState().getValue(EnergyCellBlock.CHARGE);
        final boolean charging = cell.getBlockState().getValue(EnergyCellBlock.CHARGING);
        final int rank = cell.getBlockState().getBlock() instanceof EnergyCellBlock block ? block.tier().rank() : 1;
        final float ticks = cell.getLevel() == null ? 0F : cell.getLevel().getGameTime() + partialTick;
        for (String other : EnergyCellMarks.bonesOfOtherRanks(rank)) {
            bones.hide(other, true);
        }
        for (Direction side : ChargeBar.sides()) {
            final boolean covered = (ports & 1 << side.ordinal()) != 0;
            for (int number = 1; number <= EnergyCellBlock.SEGMENTS; number++) {
                final float fill = covered ? 0 : ChargeBar.fill(number - 1, level, charging, ticks);
                final boolean alongZ = ChargeBar.runsAlongZ(side);
                final String name = ChargeBar.boneName(number, side);
                bones.hide(name, fill <= 0);
                bones.scale(name, alongZ ? 1 : fill, 1, alongZ ? fill : 1);
            }
        }
    }
}
