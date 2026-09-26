package com.morphengine.nexus.level;

import com.morphengine.nexus.api.storage.Storage;

import java.util.List;

/**
 * A network member that lends storages to its network, such as a Storage Vault
 * with its cells. When its storages or its priority change, it tells the
 * network's {@link StorageComponent} through {@link StorageComponent#refresh}.
 */
public interface StorageHost extends NetworkMember {

    /**
     * @return priority of every storage of this host; higher is filled first and emptied last
     */
    int storagePriority();

    /**
     * @return the storages the host lends right now; the network changes them only
     *         through its own operations while they are lent
     */
    List<Storage> storages();
}
