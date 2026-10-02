package com.morphengine.nexus.resource;

import com.morphengine.nexus.api.resource.ResourceGroup;
import com.morphengine.nexus.api.resource.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Objects;

/**
 * Every item in one item tag, whatever its components, as the server's tags
 * have it now.
 */
record ItemTagGroup(TagKey<Item> tag) implements ResourceGroup {

    ItemTagGroup {
        Objects.requireNonNull(tag, "tag must not be null");
    }

    @Override
    public boolean contains(final ResourceKey resource) {
        return resource instanceof ItemKey key && key.item().is(tag);
    }
}
