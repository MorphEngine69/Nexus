package com.morphengine.nexus.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import software.bernie.geckolib.animatable.GeoAnimatable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Draws a device whose model faces north and is turned by GeckoLib to the way the block faces: the ports are chosen
 * by the side of the model each cable ends up on after that turn, and the parts a port takes the place of are
 * hidden on those sides. Designed for extension: a subclass may add what else its model shows.
 */
public class FacedDeviceRenderer<T extends BlockEntity & GeoAnimatable> extends DeviceRenderer<T> {

    private final EnumProperty<Direction> facing;
    private final List<String> bonesUnderPorts;
    private final List<String> bonesShownWhenLit;

    /**
     * @param facing          the block state property that tells the way the block faces
     * @param bonesUnderPorts the prefixes of the bones a port takes the place of, see {@link #bonesUnderPorts()}
     * @param bonesShownWhenLit the bones that show only while the device is lit, see {@link #bonesShownWhenLit()}
     */
    public FacedDeviceRenderer(
            final BlockEntityRendererProvider.Context context, final AdjustableGeoModel<T> model,
            final Predicate<BlockState> isLit, final EnumProperty<Direction> facing,
            final List<String> bonesUnderPorts, final List<String> bonesShownWhenLit) {
        super(context, model, isLit);
        this.facing = Objects.requireNonNull(facing, "facing must not be null");
        this.bonesUnderPorts = List.copyOf(bonesUnderPorts);
        this.bonesShownWhenLit = List.copyOf(bonesShownWhenLit);
    }

    @Override
    protected int portsOf(final T device) {
        return DeviceRenderData.portsInModel(super.portsOf(device), device.getBlockState().getValue(facing));
    }

    @Override
    protected List<String> bonesUnderPorts() {
        return bonesUnderPorts;
    }

    @Override
    protected List<String> bonesShownWhenLit() {
        return bonesShownWhenLit;
    }
}
