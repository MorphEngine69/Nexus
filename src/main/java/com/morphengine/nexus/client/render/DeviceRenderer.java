package com.morphengine.nexus.client.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Draws a network device from its GeckoLib model: in the color of its network,
 * with a port on every side where a cable is attached, and its accents lit
 * while the device is. Designed for extension: a subclass may add what else its
 * model shows.
 */
public class DeviceRenderer<T extends BlockEntity & GeoAnimatable>
        extends GeoBlockRenderer<T, BlockEntityRenderState> {

    private static final double BLOCK_CENTER = 0.5;

    private final Predicate<BlockState> isLit;

    /**
     * @param isLit whether the device is lit in a block state
     */
    public DeviceRenderer(
            final BlockEntityRendererProvider.Context context, final GeoModel<T> model,
            final Predicate<BlockState> isLit) {
        super(context, model);
        this.isLit = Objects.requireNonNull(isLit, "isLit must not be null");
        withRenderLayer(new GlowWhenLitLayer<>(this));
    }

    @Override
    public void addRenderData(
            final T animatable, final @Nullable Void relatedObject, final BlockEntityRenderState renderState,
            final float partialTick) {
        final BlockState state = animatable.getBlockState();
        DeviceRenderData.capture(state, renderState, isLit.test(state));
    }

    @Override
    public final void adjustModelBonesForRender(
            final RenderPassInfo<BlockEntityRenderState> renderPassInfo, final BoneSnapshots snapshots) {
        final int ports = DeviceRenderData.portsOf(renderPassInfo.renderState());
        DeviceRenderData.showPorts(snapshots, ports);
        DeviceRenderData.hideUnderPorts(snapshots, ports, bonesUnderPorts());
        DeviceRenderData.hideUnlessLit(snapshots, DeviceRenderData.isLit(renderPassInfo.renderState()),
                bonesShownWhenLit());
        adjustDeviceBones(renderPassInfo.renderState(), snapshots);
        DeviceRenderData.hideItemBones(snapshots);
    }

    /**
     * A place for a device to show and size the bones of what it displays, from what its block state captured in
     * {@link #addRenderData}; the ports have been chosen by now.
     */
    protected void adjustDeviceBones(final GeoRenderState renderState, final BoneSnapshots snapshots) {
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
    protected void tryRotateByBlockstate(
            final RenderPassInfo<BlockEntityRenderState> renderPassInfo, final PoseStack poseStack) {
        final Direction facing = renderPassInfo.getOrDefaultGeckolibData(DIRECTION_FACING, Direction.NORTH);
        if (facing.getAxis() != Direction.Axis.Y) {
            super.tryRotateByBlockstate(renderPassInfo, poseStack);
            return;
        }
        poseStack.translate(0, BLOCK_CENTER, 0);
        super.tryRotateByBlockstate(renderPassInfo, poseStack);
        poseStack.translate(0, -BLOCK_CENTER, 0);
    }
}
