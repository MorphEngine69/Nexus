package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;

/**
 * Fluids show as a square of their still texture, tinted as in the world, with
 * their name and the mod that adds them in the tooltip.
 */
final class FluidResourceRenderer implements ResourceRenderer<FluidKey> {

    private static final int ICON_SIZE = 16;

    @Override
    public ResourceIcon icon(final FluidKey resource) {
        final FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(resource.fluid().getFluid().defaultFluidState());
        final FluidTintSource tint = model.fluidTintSource();
        final int color = ARGB.opaque(tint != null ? tint.colorAsStack(resource.toStack(FluidType.BUCKET_VOLUME)) : -1);
        final TextureAtlasSprite sprite = model.stillMaterial().sprite();
        return (graphics, x, y) -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, ICON_SIZE,
                ICON_SIZE, color);
    }

    @Override
    public List<Component> tooltip(final FluidKey resource) {
        final String namespace = resource.id().getNamespace();
        final String modName = ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace);
        return List.of(resource.name(),
                Component.literal(modName).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
    }
}
