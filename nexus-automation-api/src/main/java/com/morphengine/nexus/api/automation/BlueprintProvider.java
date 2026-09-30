package com.morphengine.nexus.api.automation;

import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.List;
import java.util.Set;

/**
 * The blueprints a network knows and what runs them.
 */
public interface BlueprintProvider {

    /**
     * @return every blueprint that gives {@code resource}, the preferred one
     *         first; empty when the network cannot craft it
     */
    List<Blueprint> blueprintsFor(ResourceKey resource);

    /**
     * @return the executors that run {@code blueprint}, the preferred one first;
     *         empty when none does
     */
    List<BlueprintExecutor> executorsFor(Blueprint blueprint);

    /**
     * @return every resource some blueprint gives; a snapshot
     */
    Set<ResourceKey> craftables();
}
