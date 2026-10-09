package com.morphengine.nexus.resource;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * The tags of registry entries behind resources, such as {@code c:ores/iron}
 * of an item or {@code minecraft:water} of a fluid, as the server sees them.
 */
final class ResourceTags {

    /** The shared {@code c:} tags first, as those other mods fill in too, then by id. */
    private static final Comparator<ResourceLocation> COMMON_FIRST = Comparator
            .comparing((ResourceLocation id) -> !id.getNamespace().equals("c"))
            .thenComparing(ResourceLocation::toString);

    private ResourceTags() {
    }

    static <T> List<ResourceLocation> tagsOf(final Holder<T> entry) {
        final List<ResourceLocation> tags = new ArrayList<>();
        entry.tags().forEach(tag -> tags.add(tag.location()));
        tags.sort(COMMON_FIRST);
        return List.copyOf(tags);
    }

    /**
     * @param toResource the resource of an entry in the tag
     * @return a resource for every entry in the tag, in its order; empty for an unknown tag
     */
    static <T> List<NexusResource> membersOf(final Registry<T> registry, final ResourceLocation tag,
                                             final Function<Holder<T>, NexusResource> toResource) {
        final List<NexusResource> members = new ArrayList<>();
        for (Holder<T> entry : registry.getTagOrEmpty(TagKey.create(registry.key(), tag))) {
            members.add(toResource.apply(entry));
        }
        return List.copyOf(members);
    }
}
