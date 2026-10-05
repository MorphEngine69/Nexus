package com.morphengine.nexus.client.render;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morphengine.nexus.item.DeviceBlockItem;
import net.minecraft.world.item.DyeColor;

import java.util.List;
import java.util.Locale;

/**
 * Draws a device as an item: lit, in the standard color, with no cable attached.
 */
public final class DeviceItemRenderer extends GeoItemRenderer<DeviceBlockItem> {

    private static final float CENTER = 0.5F;
    private static final float PIXELS_PER_BLOCK = 16F;

    private final float shiftTowardFront;
    private final List<String> hiddenBones;

    public DeviceItemRenderer(final DeviceBlockItem.Look look) {
        super(modelOf(look.asset(), look.color()));
        this.shiftTowardFront = look.shiftTowardFrontPixels() / PIXELS_PER_BLOCK;
        this.hiddenBones = look.hiddenBones();
        withRenderLayer(new GlowWhenLitLayer<>(this));
    }

    private static DeviceGeoModel<DeviceBlockItem> modelOf(final String asset, final DyeColor color) {
        final String textureName = color.getName().toLowerCase(Locale.ROOT);
        return new DeviceGeoModel<>(asset) {
            @Override
            protected String textureName(final GeoRenderState renderState) {
                return textureName;
            }
        };
    }

    /**
     * The model stands on the floor of its block, not on its middle as GeckoLib
     * assumes by default, so the item is not lifted half a block.
     */
    @Override
    public void adjustRenderPose(final RenderPassInfo<GeoRenderState> renderPassInfo) {
        renderPassInfo.poseStack().translate(CENTER, 0, CENTER - shiftTowardFront);
    }

    @Override
    public void adjustModelBonesForRender(
            final RenderPassInfo<GeoRenderState> renderPassInfo, final BoneSnapshots snapshots) {
        DeviceRenderData.showPorts(snapshots, 0);
        DeviceRenderData.showItemBones(snapshots);
        for (String hidden : hiddenBones) {
            snapshots.ifPresent(hidden, bone -> bone.skipRender(true));
        }
        for (String segment : ChargeBar.segmentBones()) {
            snapshots.ifPresent(segment, bone -> bone.skipRender(true));
        }
        for (String step : StorageVaultRenderer.itemHiddenBones()) {
            snapshots.ifPresent(step, bone -> bone.skipRender(true));
        }
        for (String piece : CraftingMonitorRenderer.itemHiddenBones()) {
            snapshots.ifPresent(piece, bone -> bone.skipRender(true));
        }
    }
}
