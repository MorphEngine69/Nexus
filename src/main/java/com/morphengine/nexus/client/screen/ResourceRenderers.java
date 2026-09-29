package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResourceType;
import com.morphengine.nexus.resource.ResourceTypes;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The renderer of every resource kind, fixed once when the client first draws
 * a resource. A kind without a renderer shows as its name only.
 */
final class ResourceRenderers {

    private static final Map<NexusResourceType<?>, ResourceRenderer<?>> BY_TYPE = Map.of(
            ResourceTypes.ITEM.get(), new ItemResourceRenderer(),
            ResourceTypes.FLUID.get(), new FluidResourceRenderer(),
            ResourceTypes.ENERGY.get(), new EnergyResourceRenderer());

    private ResourceRenderers() {
    }

    static ResourceIcon icon(final NexusResource resource) {
        final ResourceRenderer<NexusResource> renderer = rendererOf(resource);
        return renderer != null ? renderer.icon(resource) : ResourceIcon.NONE;
    }

    static List<Component> tooltip(final NexusResource resource) {
        final ResourceRenderer<NexusResource> renderer = rendererOf(resource);
        return renderer != null ? renderer.tooltip(resource) : List.of(resource.name());
    }

    @SuppressWarnings("unchecked")
    private static @Nullable ResourceRenderer<NexusResource> rendererOf(final NexusResource resource) {
        return (ResourceRenderer<NexusResource>) BY_TYPE.get(resource.type());
    }
}
