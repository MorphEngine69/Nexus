package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.EnergyKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Energy shows as a lightning bolt, with its name as the tooltip.
 */
final class EnergyResourceRenderer implements ResourceRenderer<EnergyKey> {

    private static final ResourceLocation SPRITE = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID,
            "resource/energy");
    private static final int ICON_SIZE = 16;
    private static final ResourceIcon ICON = (graphics, x, y) ->
            graphics.blitSprite(SPRITE, x, y, ICON_SIZE, ICON_SIZE);

    @Override
    public ResourceIcon icon(final EnergyKey resource) {
        return ICON;
    }

    @Override
    public List<Component> tooltip(final EnergyKey resource) {
        return List.of(resource.name());
    }
}
