package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Shows resources of one kind in panels: a 16x16 icon and the lines of its tooltip.
 *
 * @param <R> the kind's resources
 */
interface ResourceRenderer<R extends NexusResource> {

    /**
     * @return the resource's icon, with whatever it needs to draw looked up once,
     *         so drawing it every frame costs nothing more
     */
    ResourceIcon icon(R resource);

    /**
     * @return the tooltip without the amount; the first line is the name
     */
    List<Component> tooltip(R resource);
}
