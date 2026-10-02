package com.morphengine.nexus.level;

import java.util.List;

/**
 * Every kind of component a network is made of, fixed once at startup.
 */
public final class NetworkComponentTypes {

    /** The storages of the network seen as one. */
    public static final NetworkComponentType<StorageComponent> STORAGE =
            new NetworkComponentType<>("storage", StorageComponent::new);

    /** Blueprints and crafting tasks of the network. */
    public static final NetworkComponentType<AutocraftingComponent> AUTOCRAFTING =
            new NetworkComponentType<>("autocrafting", AutocraftingComponent::new);

    /** The access points Nexus Terminals reach the network through. */
    public static final NetworkComponentType<WirelessAccessComponent> WIRELESS_ACCESS =
            new NetworkComponentType<>("wireless_access", WirelessAccessComponent::new);

    static final List<NetworkComponentType<?>> ALL = List.of(STORAGE, AUTOCRAFTING, WIRELESS_ACCESS);

    private NetworkComponentTypes() {
    }
}
