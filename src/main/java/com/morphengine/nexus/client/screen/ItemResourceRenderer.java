package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Items look as they do in any slot and have their usual tooltip.
 */
final class ItemResourceRenderer implements ResourceRenderer<ItemKey> {

    @Override
    public ResourceIcon icon(final ItemKey resource) {
        final ItemStack stack = resource.toStack(1);
        return (graphics, x, y) -> graphics.renderItem(stack, x, y);
    }

    @Override
    public List<Component> tooltip(final ItemKey resource) {
        return Screen.getTooltipFromItem(Minecraft.getInstance(), resource.toStack(1));
    }
}
