package com.morphengine.nexus.client.render;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.AssemblerChain;
import com.morphengine.nexus.block.NetworkDeviceBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/**
 * While the player looks at an Assembler, outlines the machine it works with,
 * at the end of its {@link AssemblerChain}, in the network's color, so it is
 * plain which of the blocks around gets the inputs. The Assembler keeps its
 * own vanilla outline.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID, value = Dist.CLIENT)
public final class AssemblerMachineOutline {

    private AssemblerMachineOutline() {
    }

    @SubscribeEvent
    static void outlineMachine(final ExtractBlockOutlineRenderStateEvent event) {
        final BlockState assembler = event.getBlockState();
        if (!(assembler.getBlock() instanceof AssemblerBlock)) {
            return;
        }
        final BlockPos machine = AssemblerChain.linkOf(event.getLevel(), event.getBlockPos()).machine();
        final BlockState machineState = event.getLevel().getBlockState(machine);
        if (machineState.isAir()) {
            return;
        }
        final VoxelShape shape = machineState.getShape(event.getLevel(), machine, event.getCollisionContext());
        if (shape.isEmpty()) {
            return;
        }
        final int color = ARGB.opaque(assembler.getValue(NetworkDeviceBlock.NETWORK_COLOR).getTextureDiffuseColor());
        event.addCustomRenderer((state, collector, poseStack, levelState) -> {
            final Vec3 camera = levelState.cameraRenderState.pos;
            poseStack.pushPose();
            poseStack.translate(machine.getX() - camera.x, machine.getY() - camera.y, machine.getZ() - camera.z);
            collector.submitShapeOutline(poseStack, shape, RenderTypes.lines(), color, lineWidth(),
                    state.isTranslucent());
            poseStack.popPose();
            return false;
        });
    }

    private static float lineWidth() {
        return Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
    }
}
