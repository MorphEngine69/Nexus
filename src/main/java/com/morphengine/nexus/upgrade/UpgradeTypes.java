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
 * The registry of upgrade kinds and the kinds Nexus brings. Speed, Stack,
 * Regulator and Capacity work in Pullers and Pushers, Autocrafting in
 * Pushers, Speed in Assemblers too; the others wait for the devices of later
 * stages: Range for the wireless link, Fortune and Silk Touch for the
 * Remover. Other mods register more kinds into {@link #REGISTRY}.
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
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> RANGE =
            TYPES.register("range", NexusUpgradeType::inDevelopment);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> FORTUNE =
            TYPES.register("fortune", NexusUpgradeType::inDevelopment);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> SILK_TOUCH =
            TYPES.register("silk_touch", NexusUpgradeType::inDevelopment);
    public static final DeferredHolder<NexusUpgradeType, NexusUpgradeType> AUTOCRAFTING =
            TYPES.register("autocrafting", NexusUpgradeType::working);

    private UpgradeTypes() {
    }

    @SubscribeEvent
    static void createRegistry(final NewRegistryEvent event) {
        event.register(REGISTRY);
    }
}
