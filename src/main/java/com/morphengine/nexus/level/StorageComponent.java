package com.morphengine.nexus.level;

import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.storage.NetworkStorage;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The storages of one network seen as a single {@link NetworkStorage}: every
 * storage its {@link StorageHost}s lend, at the priority of its host.
 */
public final class StorageComponent implements NetworkComponent {

    private final NetworkStorage storage = new NetworkStorage();
    private final Map<StorageHost, Mount> mounts = new IdentityHashMap<>();

    /**
     * @return the network's storage; listeners added to it hear about every change
     */
    public NetworkStorage storage() {
        return storage;
    }

    @Override
    public void adopt(final List<NetworkMember> members) {
        final Map<StorageHost, Boolean> present = new IdentityHashMap<>();
        for (NetworkMember member : members) {
            if (member instanceof StorageHost host) {
                present.put(host, Boolean.TRUE);
            }
        }
        for (StorageHost mounted : List.copyOf(mounts.keySet())) {
            if (!present.containsKey(mounted)) {
                detach(mounted);
            }
        }
        for (StorageHost host : present.keySet()) {
            refresh(host);
        }
    }

    /**
     * Brings the network up to date with the storages and priority {@code host}
     * lends now. Storages it no longer lends leave, new ones join, kept ones move
     * to its current priority.
     */
    public void refresh(final StorageHost host) {
        Objects.requireNonNull(host, "host must not be null");
        final List<Storage> current = host.storages();
        final int priority = host.storagePriority();
        final Mount previous = mounts.getOrDefault(host, Mount.NONE);
        for (Storage lent : previous.storages()) {
            if (!containsSame(current, lent)) {
                storage.removeSource(lent);
            }
        }
        for (Storage lent : current) {
            if (!containsSame(previous.storages(), lent)) {
                storage.addSource(lent, priority);
            } else if (previous.priority() != priority) {
                storage.changePriority(lent, priority);
            }
        }
        if (current.isEmpty()) {
            mounts.remove(host);
        } else {
            mounts.put(host, new Mount(List.copyOf(current), priority));
        }
    }

    /**
     * Takes every storage of {@code host} out of the network at once, for a host
     * that is being removed; nothing can be put into its storages afterwards.
     */
    public void detach(final StorageHost host) {
        final Mount mount = mounts.remove(host);
        if (mount != null) {
            for (Storage lent : mount.storages()) {
                storage.removeSource(lent);
            }
        }
    }

    private static boolean containsSame(final List<Storage> storages, final Storage wanted) {
        for (Storage candidate : storages) {
            if (candidate == wanted) {
                return true;
            }
        }
        return false;
    }

    private record Mount(List<Storage> storages, int priority) {

        static final Mount NONE = new Mount(List.of(), 0);
    }
}
