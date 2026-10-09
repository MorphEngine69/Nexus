package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A fluid with its components, without an amount: what a fluid handler or a fluid stack is made of, and the key of a
 * fluid in a network. Immutable.
 */
public final class FluidResource {

    public static final FluidResource EMPTY = new FluidResource(FluidStack.EMPTY);
    public static final Codec<FluidResource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BuiltInRegistries.FLUID.byNameCodec().fieldOf("id").forGetter(FluidResource::getFluid),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(resource -> resource.template.getComponentsPatch()))
            .apply(instance, FluidResource::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidResource> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), FluidResource::getFluid,
            DataComponentPatch.STREAM_CODEC, resource -> resource.template.getComponentsPatch(),
            FluidResource::of);

    private final FluidStack template;

    private FluidResource(final FluidStack template) {
        this.template = template;
    }

    public static FluidResource of(final FluidStack stack) {
        Objects.requireNonNull(stack, "stack must not be null");
        return stack.isEmpty() ? EMPTY : new FluidResource(stack.copyWithAmount(1));
    }

    public static FluidResource of(final Fluid fluid) {
        Objects.requireNonNull(fluid, "fluid must not be null");
        return fluid == Fluids.EMPTY ? EMPTY : of(new FluidStack(fluid, 1));
    }

    private static FluidResource of(final Fluid fluid, final DataComponentPatch patch) {
        return fluid == Fluids.EMPTY ? EMPTY : of(new FluidStack(fluid.builtInRegistryHolder(), 1, patch));
    }

    public boolean isEmpty() {
        return template.isEmpty();
    }

    public Fluid getFluid() {
        return template.getFluid();
    }

    public Component getHoverName() {
        return template.getHoverName();
    }

    public FluidStack toStack(final int millibuckets) {
        return template.copyWithAmount(millibuckets);
    }

    public boolean is(final TagKey<Fluid> tag) {
        return template.is(tag);
    }

    public boolean matches(final FluidStack stack) {
        return FluidStack.isSameFluidSameComponents(template, stack);
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return this == other
                || other instanceof FluidResource resource
                        && FluidStack.isSameFluidSameComponents(template, resource.template);
    }

    @Override
    public int hashCode() {
        return FluidStack.hashFluidAndComponents(template);
    }

    @Override
    public String toString() {
        return "FluidResource[" + template + "]";
    }
}
