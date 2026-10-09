package com.morphengine.nexus.resource;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The registry of resource kinds and the kinds Nexus brings: items, fluids and
 * energy. Other mods register more kinds into {@link #REGISTRY}.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class ResourceTypes {

    public static final ResourceKey<Registry<NexusResourceType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "resource_type"));

    public static final Registry<NexusResourceType<?>> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY).create();

    public static final DeferredRegister<NexusResourceType<?>> TYPES =
            DeferredRegister.create(REGISTRY_KEY, Nexus.MOD_ID);

    public static final DeferredHolder<NexusResourceType<?>, NexusResourceType<ItemKey>> ITEM = TYPES.register(
            "item", () -> new NexusResourceType<>(ItemKey.CODEC, ItemKey.STREAM_CODEC, AmountUnit.ITEMS));

    public static final DeferredHolder<NexusResourceType<?>, NexusResourceType<FluidKey>> FLUID = TYPES.register(
            "fluid", () -> new NexusResourceType<>(FluidKey.CODEC, FluidKey.STREAM_CODEC, AmountUnit.MILLIBUCKETS));

    public static final DeferredHolder<NexusResourceType<?>, NexusResourceType<EnergyKey>> ENERGY = TYPES.register(
            "energy", () -> new NexusResourceType<>(EnergyKey.CODEC, EnergyKey.STREAM_CODEC, AmountUnit.ENERGY));

    private ResourceTypes() {
    }

    /**
     * @return the registry id of {@code type}
     * @throws IllegalArgumentException if the type is not registered
     */
    public static ResourceLocation idOf(final NexusResourceType<?> type) {
        final ResourceLocation id = REGISTRY.getKey(type);
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
