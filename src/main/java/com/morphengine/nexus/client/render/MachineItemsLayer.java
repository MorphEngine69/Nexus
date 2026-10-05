package com.morphengine.nexus.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Draws on the bones of a machine the items that the server says it shows: what an Alloy Smelter has in its input slots
 * rides its rails, and the result stands in its mold. A place with no item draws nothing, so the bone moves empty.
 */
final class MachineItemsLayer extends GeoRenderLayer<MachineBlockEntity, Void, BlockEntityRenderState> {

    private static final List<String> BONES = List.of("item_left", "item_middle", "item_right", "result");
    private static final DataTicket<List<Shown>> SHOWN =
            DataTicket.create("machine_shown_items", new TypeToken<>() { });
    private static final float ITEM_SIZE = 0.17F;
    private static final float RESULT_SIZE = 0.24F;
    private static final int RESULT_PLACE = 3;
    private static final float TOWARD_FRONT = -0.03F;

    private final ItemModelResolver itemModels;

    MachineItemsLayer(
            final GeoRenderer<MachineBlockEntity, Void, BlockEntityRenderState> renderer,
            final ItemModelResolver itemModels) {
        super(renderer);
        this.itemModels = itemModels;
    }

    @Override
    public void addRenderData(
            final MachineBlockEntity animatable, final @Nullable Void relatedObject,
            final BlockEntityRenderState renderState, final float partialTick) {
        final List<Shown> shown = new ArrayList<>(BONES.size());
        for (int place = 0; place < BONES.size(); place++) {
            final ItemStack item = animatable.shownItem(place);
            if (!item.isEmpty()) {
                final ItemStackRenderState model = new ItemStackRenderState();
                itemModels.updateForTopItem(model, item, ItemDisplayContext.FIXED, animatable.getLevel(), null, place);
                shown.add(new Shown(BONES.get(place), model, place == RESULT_PLACE ? RESULT_SIZE : ITEM_SIZE));
            }
        }
        renderState.addGeckolibData(SHOWN, shown);
    }

    @Override
    public void addPerBoneRender(
            final RenderPassInfo<BlockEntityRenderState> renderPassInfo,
            final BiConsumer<GeoBone, PerBoneRender<BlockEntityRenderState>> consumer) {
        for (Shown item : renderPassInfo.renderState().getOrDefaultGeckolibData(SHOWN, List.of())) {
            renderPassInfo.model().getBone(item.bone()).ifPresent(bone -> consumer.accept(bone,
                    (info, posedBone, tasks) -> submit(info.poseStack(), item, tasks, info.packedLight())));
        }
    }

    private static void submit(
            final PoseStack poseStack, final Shown item, final SubmitNodeCollector tasks, final int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0, 0, TOWARD_FRONT);
        poseStack.scale(item.size(), item.size(), item.size());
        item.model().submit(poseStack, tasks, packedLight, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private record Shown(String bone, ItemStackRenderState model, float size) {
    }
}
