package com.morphengine.nexus.resource;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.api.resource.ResourceGroup;
import com.morphengine.nexus.transfer.FluidResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A fluid with its components, never empty. Amounts of it are in millibuckets.
 */
public record FluidKey(FluidResource fluid) implements NexusResource {

    public static final MapCodec<FluidKey> CODEC = FluidResource.CODEC.xmap(FluidKey::new, FluidKey::fluid)
            .fieldOf("fluid");
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidKey> STREAM_CODEC =
            FluidResource.STREAM_CODEC.map(FluidKey::new, FluidKey::fluid);

    public FluidKey {
        Objects.requireNonNull(fluid, "fluid must not be null");
        if (fluid.isEmpty()) {
            throw new IllegalArgumentException("fluid resource must not be empty");
        }
    }

    public FluidStack toStack(final int millibuckets) {
        return fluid.toStack(millibuckets);
    }

    @Override
    public NexusResourceType<?> type() {
        return ResourceTypes.FLUID.get();
    }

    @Override
    public Component name() {
        return fluid.getHoverName();
    }

    @Override
    public ResourceLocation id() {
        return BuiltInRegistries.FLUID.getKey(fluid.getFluid());
    }

    @Override
    public List<ResourceLocation> tags() {
        return ResourceTags.tagsOf(BuiltInRegistries.FLUID.wrapAsHolder(fluid.getFluid()));
    }

    @Override
    public List<NexusResource> membersOf(final ResourceLocation tag) {
        return ResourceTags.membersOf(BuiltInRegistries.FLUID, tag, entry ->
                new FluidKey(FluidResource.of(entry.value())));
    }

    @Override
    public Optional<ResourceGroup> tagGroup(final ResourceLocation tag) {
        return Optional.of(new FluidTagGroup(TagKey.create(Registries.FLUID, tag)));
    }
}
