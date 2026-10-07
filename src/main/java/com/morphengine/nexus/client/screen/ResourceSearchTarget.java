package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.search.SearchTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * A resource of a terminal as a search looks at it. What is costly to find out, the tags and the tooltip, is found out
 * the first time a search asks for it and kept while the target lives. Client thread only.
 */
final class ResourceSearchTarget implements SearchTarget {

    private final NexusResource resource;
    private @Nullable String name;
    private @Nullable String modName;
    private @Nullable List<String> tags;
    private @Nullable String tooltip;

    ResourceSearchTarget(final NexusResource resource) {
        this.resource = resource;
    }

    @Override
    public String name() {
        if (name == null) {
            name = lower(resource.name().getString());
        }
        return name;
    }

    @Override
    public String modId() {
        return resource.id().getNamespace();
    }

    @Override
    public String modName() {
        if (modName == null) {
            final String namespace = resource.id().getNamespace();
            modName = lower(ModList.get().getModContainerById(namespace)
                    .map(container -> container.getModInfo().getDisplayName())
                    .orElse(namespace));
        }
        return modName;
    }

    @Override
    public Collection<String> tags() {
        if (tags == null) {
            tags = resource.tags().stream().map(Identifier::toString).map(ResourceSearchTarget::lower).toList();
        }
        return tags;
    }

    @Override
    public String tooltip() {
        if (tooltip == null) {
            final StringBuilder text = new StringBuilder();
            for (Component line : ResourceRenderers.tooltip(resource)) {
                text.append(line.getString()).append('\n');
            }
            tooltip = lower(text.toString());
        }
        return tooltip;
    }

    private static String lower(final String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
