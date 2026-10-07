package com.morphengine.nexus.upgrade;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The registry of upgrade kinds and the kinds Nexus brings. Speed, Stack and
 * Capacity work in Pullers, Pushers, Placers and Removers, Regulator in
 * Pullers and Pushers, Autocrafting in Pushers and Placers, Fortune and Silk
 * Touch in Removers, Range in Nexus Links, Speed in Assemblers and the Coal
 * Generator too, Efficiency and Buffer in machines and generators, Void, one at most, in Storage Vaults and External
 * Vaults, and Chunk Loader, one at most, in every device that takes upgrades. Other mods register more kinds into
 * {@link #REGISTRY}.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class UpgradeTypes {

    public static final ResourceKey<Registry<NexusUpgradeType>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "upgrade_type"));

    public static final Registry<NexusUpgradeType> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY).create();

    public static final DeferredRegister<NexusUpgradeType> TYPES = DeferredRegister.create(REGISTRY_KEY, Nexus.MOD_ID);

    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> SPEED =
            TYPES.register("speed", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> STACK =
            TYPES.register("stack", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> REGULATOR =
            TYPES.register("regulator", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> CAPACITY =
            TYPES.register("capacity", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> EFFICIENCY =
            TYPES.register("efficiency", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> BUFFER =
            TYPES.register("buffer", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> RANGE =
            TYPES.register("range", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> FORTUNE =
            TYPES.register("fortune", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> SILK_TOUCH =
            TYPES.register("silk_touch", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> AUTOCRAFTING =
            TYPES.register("autocrafting", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> CHUNK_LOADER =
            TYPES.register("chunk_loader", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> DIMENSION =
            TYPES.register("dimension", NexusUpgradeType::working);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> VOID =
            TYPES.register("void", NexusUpgradeType::working);

    private UpgradeTypes() {
    }

    @SubscribeEvent
    static void createRegistry(final NewRegistryEvent event) {
        event.register(REGISTRY);
    }
}
