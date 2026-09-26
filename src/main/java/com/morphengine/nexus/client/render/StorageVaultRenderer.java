package com.morphengine.nexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.VaultLamp;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Draws the lamps of a Storage Vault over the cell bays of its model: green
 * while a cell has room, orange when it ran out of types or bytes, red when it
 * ran out of both, dark for an empty bay or a network without energy. The
 * model draws every bay with a cartridge in it, so the front never looks empty.
 */
public final class StorageVaultRenderer
        implements BlockEntityRenderer<StorageVaultBlockEntity, StorageVaultRenderer.LampState> {

    private static final RenderType LAMP = RenderTypes.entitySolid(
            Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "textures/block/storage_vault/lamp.png"));
    private static final int[] LAMP_ARGB = {0, 0xFF46E06A, 0xFFFFA23A, 0xFFFF4238};

    private static final float PIXEL = 1.0F / 16;
    private static final int COLUMNS = 4;
    /** Cartridges are 2 pixels wide, 3 apart, the first ending half a pixel inside the bay's edge at x 14. */
    private static final float FIRST_CELL_RIGHT = 13.5F;
    private static final float CELL_WIDTH = 2;
    private static final float COLUMN_PITCH = 3;
    /** Cartridges are 3 pixels tall, 4 apart; the lamp is the top pixel, the top one from y 14.5 to 15.5. */
    private static final float TOP_LAMP = 14.5F;
    private static final float ROW_PITCH = 4;
    /** Just in front of the cartridges, recessed a pixel behind the front of the vault. */
    private static final float FRONT = (1 - 0.02F) * PIXEL;
    private static final float HALF = 0.5F;
    private static final float HALF_TURN = 180;

    @Override
    public LampState createRenderState() {
        return new LampState();
    }

    @Override
    public void extractRenderState(
            final StorageVaultBlockEntity vault, final LampState state, final float partialTicks,
            final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(vault, state, breakProgress);
        state.facing = vault.getBlockState().getValue(StorageVaultBlock.FACING);
        for (int slot = 0; slot < StorageVaultBlockEntity.SLOTS; slot++) {
            state.lamps[slot] = vault.lampAt(slot);
        }
    }

    @Override
    public void submit(
            final LampState state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(HALF, HALF, HALF);
        poseStack.mulPose(Axis.YP.rotationDegrees(HALF_TURN - state.facing.toYRot()));
        poseStack.translate(-HALF, -HALF, -HALF);
        collector.submitCustomGeometry(poseStack, LAMP, (pose, buffer) -> {
            for (int slot = 0; slot < StorageVaultBlockEntity.SLOTS; slot++) {
                final VaultLamp lamp = state.lamps[slot];
                if (lamp != VaultLamp.OFF) {
                    drawLamp(pose, buffer, slot, LAMP_ARGB[lamp.ordinal()]);
                }
            }
        });
        poseStack.popPose();
    }

    /**
     * The lamp is the top right pixel of its cartridge as seen from the front,
     * which faces north in the model: the lowest x of the cartridge.
     */
    private static void drawLamp(final PoseStack.Pose pose, final VertexConsumer buffer, final int slot,
                                 final int argb) {
        final int column = slot % COLUMNS;
        final int row = slot / COLUMNS;
        final float left = (FIRST_CELL_RIGHT - CELL_WIDTH - COLUMN_PITCH * column) * PIXEL;
        final float right = left + PIXEL;
        final float bottom = (TOP_LAMP - ROW_PITCH * row) * PIXEL;
        final float top = bottom + PIXEL;
        vertex(pose, buffer, right, top, argb);
        vertex(pose, buffer, right, bottom, argb);
        vertex(pose, buffer, left, bottom, argb);
        vertex(pose, buffer, left, top, argb);
    }

    private static void vertex(final PoseStack.Pose pose, final VertexConsumer buffer, final float x, final float y,
                               final int argb) {
        buffer.addVertex(pose, x, y, FRONT).setColor(argb).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, -1);
    }

    /**
     * What the renderer needs of a vault for one frame.
     */
    public static final class LampState extends BlockEntityRenderState {

        private final VaultLamp[] lamps = new VaultLamp[StorageVaultBlockEntity.SLOTS];
        private Direction facing = Direction.NORTH;

        LampState() {
            Arrays.fill(lamps, VaultLamp.OFF);
        }
    }
}
