package com.morphengine.nexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.List;

/**
 * Draws on the bones of a machine the items that the server says it shows: what an Alloy Smelter has in its input slots
 * rides its rails, and the result stands in its mold. A place with no item draws nothing, so the bone moves empty.
 */
final class MachineItemsLayer extends GeoRenderLayer<MachineBlockEntity> {

    private static final List<String> BONES = List.of("item_left", "item_middle", "item_right", "result");
    private static final float ITEM_SIZE = 0.17F;
    private static final float RESULT_SIZE = 0.24F;
    private static final int RESULT_PLACE = 3;
    private static final float TOWARD_FRONT = -0.03F;

    private final ItemRenderer itemRenderer;

    MachineItemsLayer(final GeoRenderer<MachineBlockEntity> renderer, final ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void renderForBone(
            final PoseStack poseStack, final MachineBlockEntity machine, final GeoBone bone,
            final RenderType renderType, final MultiBufferSource bufferSource, final @Nullable VertexConsumer buffer,
            final float partialTick, final int packedLight, final int packedOverlay) {
        final int place = BONES.indexOf(bone.getName());
        if (place < 0) {
            return;
        }
        final ItemStack item = machine.shownItem(place);
        if (item.isEmpty()) {
            return;
        }
        final float size = place == RESULT_PLACE ? RESULT_SIZE : ITEM_SIZE;
        poseStack.pushPose();
        RenderUtil.translateToPivotPoint(poseStack, bone);
        poseStack.translate(0, 0, TOWARD_FRONT);
        poseStack.scale(size, size, size);
        itemRenderer.renderStatic(item, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack,
                bufferSource, machine.getLevel(), place);
        poseStack.popPose();
    }
}
