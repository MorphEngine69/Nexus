package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;

import java.util.Set;

/**
 * A slot of a device that takes more than {@link Permission#CONFIGURE} to
 * touch, such as a cell slot, whose cell carries resources of the network.
 */
interface GuardedSlot {

    Set<Permission> permissions();
}
