package com.morphengine.nexus.resource;

import com.morphengine.nexus.api.resource.ResourceGroup;
import com.morphengine.nexus.api.resource.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

/**
 * Every fluid in one fluid tag, whatever its components, as the server's tags
 * have it now.
 */
record FluidTagGroup(TagKey<Fluid> tag) implements ResourceGroup {

    FluidTagGroup {
        Objects.requireNonNull(tag, "tag must not be null");
    }

    @Override
    public boolean contains(final ResourceKey resource) {
        return resource instanceof FluidKey key && key.fluid().is(tag);
    }
}
