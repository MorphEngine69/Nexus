package com.morphengine.nexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Draws a network device from its GeckoLib model: in the color of its network,
 * with a port on every side where a cable is attached, and its accents lit
 * while the device is. Designed for extension: a subclass may add what else its
 * model shows.
 */
public class DeviceRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {

    private static final double BLOCK_CENTER = 0.5;

    private final Predicate<BlockState> isLit;

    /**
     * @param isLit whether the device is lit in a block state
     */
    public DeviceRenderer(
            final BlockEntityRendererProvider.Context context, final AdjustableGeoModel<T> model,
            final Predicate<BlockState> isLit) {
        super(model);
        this.isLit = Objects.requireNonNull(isLit, "isLit must not be null");
        model.adjustWith(new AdjustableGeoModel.BoneAdjuster<T>() {
            @Override
            public void adjust(final T animatable, final Bones bones, final float partialTick) {
                adjustModelBones(animatable, bones, partialTick);
            }
        });
        addRenderLayer(new GlowWhenLitLayer<T>(this, new Predicate<T>() {
            @Override
            public boolean test(final T device) {
                return DeviceRenderer.this.isLit.test(device.getBlockState());
            }
        }));
    }

    private void adjustModelBones(final T device, final Bones bones, final float partialTick) {
        final BlockState state = device.getBlockState();
        final int ports = portsOf(device);
        DeviceRenderData.showPorts(bones, ports);
        DeviceRenderData.hideUnderPorts(bones, ports, bonesUnderPorts());
        DeviceRenderData.hideUnlessLit(bones, isLit.test(state), bonesShownWhenLit());
        adjustDeviceBones(device, bones, ports, partialTick);
        DeviceRenderData.hideItemBones(bones);
    }

    /**
     * @return the sides with a port, one bit for each by {@link Direction#ordinal()}, as the model has them
     */
    protected int portsOf(final T device) {
        return DeviceRenderData.portsOf(device.getBlockState());
    }

    /**
     * A place for a device to show and size the bones of what it displays, from its block state and its own state;
     * the ports have been chosen by now.
     *
     * @param ports the sides with a port, as {@link #portsOf} says
     */
    protected void adjustDeviceBones(final T device, final Bones bones, final int ports, final float partialTick) {
    }

    /**
     * @return the prefixes of the bones, named {@code <prefix>_<side>}, that a port on their side takes the place of
     */
    protected List<String> bonesUnderPorts() {
        return List.of();
    }

    /**
     * @return the bones that show only while the device is lit; a model cannot start with them hidden, so without
     *         this they would show for a moment before the first animation takes hold
     */
    protected List<String> bonesShownWhenLit() {
        return List.of();
    }

    /**
     * GeckoLib turns a model about the floor of its block; one that faces up or down turns about the
     * middle of the block instead, so it stays inside it.
     */
    @Override
    protected void rotateBlock(final Direction facing, final PoseStack poseStack) {
        if (facing.getAxis() != Direction.Axis.Y) {
            super.rotateBlock(facing, poseStack);
            return;
        }
        poseStack.translate(0, BLOCK_CENTER, 0);
        super.rotateBlock(facing, poseStack);
        poseStack.translate(0, -BLOCK_CENTER, 0);
    }
}
