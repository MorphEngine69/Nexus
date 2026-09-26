package com.morphengine.nexus.resource;

import com.morphengine.nexus.api.resource.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A resource of the game: what the network stores, saves, sends to clients and
 * shows to players. Every resource the mod creates is one.
 */
public interface NexusResource extends ResourceKey {

    @Override
    NexusResourceType<?> type();

    /**
     * @return the name players see, such as an item's hover name
     */
    Component name();

    /**
     * @return the registry id of the underlying item or fluid; its namespace is
     *         the mod that adds it
     */
    Identifier id();
}
