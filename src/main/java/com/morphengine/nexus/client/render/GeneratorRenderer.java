package com.morphengine.nexus.client.render;

import com.morphengine.nexus.block.GeneratorBlock;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.processing.MachinePhase;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import java.util.List;

/**
 * Draws a generator from the model of its kind, in the color of its network, with a port on every side where a cable is
 * attached and the front free of one, and its accents lit while there is energy.
 */
public final class GeneratorRenderer extends FacedDeviceRenderer<GeneratorBlockEntity> {

    public GeneratorRenderer(final BlockEntityRendererProvider.Context context) {
        super(context, new GeneratorGeoModel(GeneratorKind.COAL.id()),
                state -> state.getValue(GeneratorBlock.PHASE).isLit(), GeneratorBlock.FACING, List.of(), List.of());
    }

    /**
     * @return what follows the color in the name of the texture of a generator in {@code phase}: the name of the phase
     *         for a kind whose texture is animated by phase, nothing for the rest
     */
    static String stateTexture(final GeneratorKind kind, final MachinePhase phase) {
        return kind.hasPhaseTextures() ? "_" + textureOf(phase) : "";
    }

    private static String textureOf(final MachinePhase phase) {
        return switch (phase) {
            case OFF -> "idle";
            case STANDBY, COOLING -> "online";
            case ACTIVE -> "active";
        };
    }
}
