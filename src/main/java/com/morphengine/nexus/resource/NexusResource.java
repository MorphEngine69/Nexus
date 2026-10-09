package com.morphengine.nexus.resource;

import com.morphengine.nexus.api.resource.ResourceGroup;
import com.morphengine.nexus.api.resource.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    ResourceLocation id();

    /**
     * @return the tags of the underlying item or fluid, the shared {@code c:}
     *         tags first; empty for a resource without tags
     */
    default List<ResourceLocation> tags() {
        return List.of();
    }

    /**
     * @return a resource of the same kind for every entry in {@code tag}, without
     *         components; empty for an unknown tag or a resource without tags
     */
    default List<NexusResource> membersOf(final ResourceLocation tag) {
        return List.of();
    }

    /**
     * @return every resource of the same kind in {@code tag}, as one entry a
     *         filter can list; empty for a resource without tags
     */
    default Optional<ResourceGroup> tagGroup(final ResourceLocation tag) {
        return Optional.empty();
    }

    /**
     * Steps through the tags of this resource and then to none, and from none
     * back to the first tag.
     *
     * @param current the tag chosen now; {@code null} for none
     * @param step    one to go forwards, minus one to go backwards
     * @return the tag after {@code current}; {@code null} for none
     */
    default @Nullable ResourceLocation nextTag(final @Nullable ResourceLocation current, final int step) {
        final List<@Nullable ResourceLocation> choices = new ArrayList<>(tags());
        choices.add(null);
        final int index = choices.indexOf(current);
        return choices.get(Math.floorMod(index + step, choices.size()));
    }
}
