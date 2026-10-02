package com.morphengine.nexus.transport;

/**
 * One entry of what a Pusher delivers: a resource it keeps stocked, or a group
 * of which it delivers whatever members its network holds.
 */
public sealed interface PushEntry permits StockEntry, GroupEntry {
}
