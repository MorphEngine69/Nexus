package com.morphengine.nexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.item.DeviceBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.List;
import java.util.Locale;

/**
 * Draws a device as an item: lit, in the standard color, with no cable attached.
 */
public final class DeviceItemRenderer extends GeoItemRenderer<DeviceBlockItem> {

    private static final float PIXELS_PER_BLOCK = 16F;
    private static final float GECKOLIB_LIFT = 0.51F;

    private final float shiftTowardFront;
    private final ItemLook itemLook;

    public DeviceItemRenderer(final DeviceBlockItem.Look look) {
        this(look, new ItemLook(look));
    }

    private DeviceItemRenderer(final DeviceBlockItem.Look look, final ItemLook itemLook) {
        super(new ItemDeviceGeoModel(look, itemLook));
        this.shiftTowardFront = look.shiftTowardFrontPixels() / PIXELS_PER_BLOCK;
        this.itemLook = itemLook;
        addRenderLayer(new GlowWhenLitLayer<>(this, item -> true));
    }

    @Override
    public void renderByItem(
            final ItemStack stack, final ItemDisplayContext transformType, final PoseStack poseStack,
            final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
        itemLook.perspective = transformType;
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }

    /**
     * The model stands on the floor of its block, not on its middle as GeckoLib
     * assumes by default, so the item is not lifted half a block.
     */
    @Override
    public void preRender(
            final PoseStack poseStack, final DeviceBlockItem item, final BakedGeoModel model,
            final @Nullable MultiBufferSource bufferSource, final @Nullable VertexConsumer buffer,
            final boolean isReRender, final float partialTick, final int packedLight, final int packedOverlay,
            final int colour) {
        super.preRender(poseStack, item, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        if (!isReRender) {
            poseStack.translate(0, -GECKOLIB_LIFT, -shiftTowardFront);
        }
    }

    /**
     * What the item model needs to know about the frame it draws.
     */
    static final class ItemLook {

        private final DeviceBlockItem.Look look;
        private ItemDisplayContext perspective = ItemDisplayContext.NONE;

        ItemLook(final DeviceBlockItem.Look look) {
            this.look = look;
        }

        String textureName() {
            final String color = perspective == ItemDisplayContext.GUI ? look.slotColor().getName()
                    : look.color().getName();
            return color.toLowerCase(Locale.ROOT) + look.textureSuffix();
        }

        List<String> hiddenBones() {
            return look.hiddenBones();
        }
    }

    /**
     * The model of the item: the texture of the color the look says, the bones the look hides.
     */
    private static final class ItemDeviceGeoModel extends AdjustableGeoModel<DeviceBlockItem> {

        private final String asset;
        private final ItemLook itemLook;

        ItemDeviceGeoModel(final DeviceBlockItem.Look look, final ItemLook itemLook) {
            this.asset = look.asset();
            this.itemLook = itemLook;
            adjustWith(this::adjust);
        }

        @Override
        public ResourceLocation getModelResource(final DeviceBlockItem item) {
            return ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "geo/block/" + asset + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(final DeviceBlockItem item) {
            return ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID,
                    "textures/geo/" + asset + "/" + itemLook.textureName() + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(final DeviceBlockItem item) {
            return ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID,
                    "animations/block/" + asset + ".animation.json");
        }

        private void adjust(final DeviceBlockItem item, final Bones bones, final float partialTick) {
            DeviceRenderData.showPorts(bones, 0);
            DeviceRenderData.showItemBones(bones);
            for (String hidden : itemLook.hiddenBones()) {
                bones.hide(hidden, true);
            }
            for (String segment : ChargeBar.segmentBones()) {
                bones.hide(segment, true);
            }
            for (String step : StorageVaultRenderer.itemHiddenBones()) {
                bones.hide(step, true);
            }
            for (String piece : CraftingMonitorRenderer.itemHiddenBones()) {
                bones.hide(piece, true);
            }
        }
    }
}
