package com.morphengine.nexus.resource;

import com.morphengine.nexus.api.resource.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

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

    /**
     * @return the tags of the underlying item or fluid, the shared {@code c:}
     *         tags first; empty for a resource without tags
     */
    default List<Identifier> tags() {
        return List.of();
    }

    /**
     * @return a resource of the same kind for every entry in {@code tag}, without
     *         components; empty for an unknown tag or a resource without tags
     */
    default List<NexusResource> membersOf(final Identifier tag) {
        return List.of();
    }
}
