package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;

/**
 * Fluids show as a square of their still texture, tinted as in the world, with
 * their name and the mod that adds them in the tooltip.
 */
final class FluidResourceRenderer implements ResourceRenderer<FluidKey> {

    private static final int ICON_SIZE = 16;
    private static final float COLOR_SCALE = 255.0F;

    @Override
    public ResourceIcon icon(final FluidKey resource) {
        final FluidStack stack = resource.toStack(FluidType.BUCKET_VOLUME);
        final IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(resource.fluid().getFluid());
        final int color = FastColor.ARGB32.opaque(extensions.getTintColor(stack));
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extensions.getStillTexture(stack));
        final float red = FastColor.ARGB32.red(color) / COLOR_SCALE;
        final float green = FastColor.ARGB32.green(color) / COLOR_SCALE;
        final float blue = FastColor.ARGB32.blue(color) / COLOR_SCALE;
        return (graphics, x, y) -> graphics.blit(x, y, 0, ICON_SIZE, ICON_SIZE, sprite, red, green, blue, 1.0F);
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
