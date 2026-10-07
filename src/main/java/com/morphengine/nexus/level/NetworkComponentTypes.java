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

    /** Who draws and who supplies the energy of the network. */
    public static final NetworkComponentType<EnergyAccountComponent> ENERGY_ACCOUNT =
            new NetworkComponentType<>("energy_account", EnergyAccountComponent::new);

    /** The blocks that keep chunks loaded for the network. */
    public static final NetworkComponentType<ChunkLoadersComponent> CHUNK_LOADERS =
            new NetworkComponentType<>("chunk_loaders", ChunkLoadersComponent::new);

    static final List<NetworkComponentType<?>> ALL =
            List.of(STORAGE, AUTOCRAFTING, WIRELESS_ACCESS, ENERGY_ACCOUNT, CHUNK_LOADERS);

    private NetworkComponentTypes() {
    }
}
