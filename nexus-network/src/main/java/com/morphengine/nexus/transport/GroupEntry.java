package com.morphengine.nexus.transport;

import com.morphengine.nexus.api.resource.ResourceGroup;

import java.util.Objects;

/**
 * A group a Pusher delivers the members of, such as an item tag in its
 * whitelist: whichever member the network holds goes, as long as the storage
 * takes more. No amount is kept, since the group names no one resource to
 * count.
 */
public record GroupEntry(ResourceGroup group) implements PushEntry {

    public GroupEntry {
        Objects.requireNonNull(group, "group must not be null");
    }
}
