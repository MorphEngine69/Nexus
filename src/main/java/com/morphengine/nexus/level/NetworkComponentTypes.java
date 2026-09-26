package com.morphengine.nexus.level;

import java.util.List;

/**
 * Every kind of component a network is made of, fixed once at startup.
 */
public final class NetworkComponentTypes {

    /** The storages of the network seen as one. */
    public static final NetworkComponentType<StorageComponent> STORAGE =
            new NetworkComponentType<>("storage", StorageComponent::new);

    static final List<NetworkComponentType<?>> ALL = List.of(STORAGE);

    private NetworkComponentTypes() {
    }
}
