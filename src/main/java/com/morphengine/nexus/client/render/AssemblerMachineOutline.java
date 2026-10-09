package com.morphengine.nexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.NetworkDeviceBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.joml.Vector3f;

/**
 * While the player looks at an Assembler, outlines the machine its face
 * touches in the network's color, so it is plain which of the blocks around
 * it gets the inputs. The Assembler keeps its own vanilla outline.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID, value = Dist.CLIENT)
public final class AssemblerMachineOutline {

    private static final float OUTLINE_ALPHA = 1.0F;
    private static final float COLOR_SCALE = 255.0F;

    private AssemblerMachineOutline() {
    }

    @SubscribeEvent
    static void outlineMachine(final RenderHighlightEvent.Block event) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        final BlockState assembler = level.getBlockState(event.getTarget().getBlockPos());
        if (!(assembler.getBlock() instanceof AssemblerBlock)) {
            return;
        }
        final BlockPos machine = event.getTarget().getBlockPos().relative(assembler.getValue(AssemblerBlock.FACING));
        final BlockState machineState = level.getBlockState(machine);
        if (machineState.isAir()) {
            return;
        }
        final VoxelShape shape = machineState.getShape(level, machine, CollisionContext.empty());
        if (shape.isEmpty()) {
            return;
        }
        final int color = FastColor.ARGB32.opaque(
                assembler.getValue(NetworkDeviceBlock.NETWORK_COLOR).getTextureDiffuseColor());
        final Vec3 camera = event.getCamera().getPosition();
        drawLines(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()), shape,
                machine.getX() - camera.x, machine.getY() - camera.y, machine.getZ() - camera.z, color);
    }

    private static void drawLines(
            final PoseStack poseStack, final VertexConsumer lines, final VoxelShape shape, final double x,
            final double y, final double z, final int color) {
        final PoseStack.Pose pose = poseStack.last();
        final float red = FastColor.ARGB32.red(color) / COLOR_SCALE;
        final float green = FastColor.ARGB32.green(color) / COLOR_SCALE;
        final float blue = FastColor.ARGB32.blue(color) / COLOR_SCALE;
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            final Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
            lines.addVertex(pose, (float) (x1 + x), (float) (y1 + y), (float) (z1 + z))
                    .setColor(red, green, blue, OUTLINE_ALPHA).setNormal(pose, normal.x(), normal.y(), normal.z());
            lines.addVertex(pose, (float) (x2 + x), (float) (y2 + y), (float) (z2 + z))
                    .setColor(red, green, blue, OUTLINE_ALPHA).setNormal(pose, normal.x(), normal.y(), normal.z());
        });
    }
}
