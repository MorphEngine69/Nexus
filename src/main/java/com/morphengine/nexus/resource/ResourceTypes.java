package com.morphengine.nexus.resource;

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
 * The registry of resource kinds and the two kinds Nexus brings: items and
 * fluids. Other mods register more kinds into {@link #REGISTRY}.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class ResourceTypes {

    public static final ResourceKey<Registry<NexusResourceType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "resource_type"));

    public static final Registry<NexusResourceType<?>> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY).create();

    public static final DeferredRegister<NexusResourceType<?>> TYPES =
            DeferredRegister.create(REGISTRY_KEY, Nexus.MOD_ID);

    public static final DeferredHolder<NexusResourceType<?>, NexusResourceType<ItemKey>> ITEM = TYPES.register(
            "item", () -> new NexusResourceType<>(ItemKey.CODEC, ItemKey.STREAM_CODEC, AmountUnit.ITEMS));

    public static final DeferredHolder<NexusResourceType<?>, NexusResourceType<FluidKey>> FLUID = TYPES.register(
            "fluid", () -> new NexusResourceType<>(FluidKey.CODEC, FluidKey.STREAM_CODEC, AmountUnit.MILLIBUCKETS));

    private ResourceTypes() {
    }

    /**
     * @return the registry id of {@code type}
     * @throws IllegalArgumentException if the type is not registered
     */
    public static Identifier idOf(final NexusResourceType<?> type) {
        final Identifier id = REGISTRY.getKey(type);
        if (id == null) {
            throw new IllegalArgumentException("resource type is not registered: " + type);
        }
        return id;
    }

    @SubscribeEvent
    static void createRegistry(final NewRegistryEvent event) {
        event.register(REGISTRY);
    }
}
