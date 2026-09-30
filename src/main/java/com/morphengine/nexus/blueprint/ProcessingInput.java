package com.morphengine.nexus.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One input of processing: the resource encoded and how much of it a run
 * takes, and the tag whose members substitute for it when substitutes are
 * allowed.
 *
 * @param tag the tag of items or fluids, such as {@code c:ores/iron};
 *            {@code null} when the input takes nothing but its resource
 */
public record ProcessingInput(ResourceAmount amount, @Nullable Identifier tag) {

    /** Saved with the fields of the amount, so inputs saved before tags existed read as they were. */
    public static final Codec<ProcessingInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    NexusResources.CODEC.fieldOf("resource").forGetter(ProcessingInput::resource),
                    Codec.LONG.fieldOf("amount").forGetter(input -> input.amount().amount()),
                    Identifier.CODEC.optionalFieldOf("tag").forGetter(ProcessingInput::tagIfAny))
            .apply(instance, ProcessingInput::of));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingInput> STREAM_CODEC = StreamCodec.composite(
            NexusResources.AMOUNT_STREAM_CODEC, ProcessingInput::amount,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), ProcessingInput::tagIfAny,
            (amount, tag) -> new ProcessingInput(amount, tag.orElse(null)));

    public ProcessingInput {
        Objects.requireNonNull(amount, "amount must not be null");
    }

    /**
     * @return an input that takes nothing but {@code exact}
     */
    public static ProcessingInput of(final ResourceAmount exact) {
        return new ProcessingInput(exact, null);
    }

    private static ProcessingInput of(final NexusResource resource, final long amount,
                                      final Optional<Identifier> tag) {
        return new ProcessingInput(new ResourceAmount(resource, amount), tag.orElse(null));
    }

    public NexusResource resource() {
        return NexusResources.of(amount.resource());
    }

    private Optional<Identifier> tagIfAny() {
        return Optional.ofNullable(tag);
    }

    /**
     * @return the resources the input takes: its own, then, when substitutes
     *         are allowed, every other member of its tag
     */
    public List<ResourceKey> options(final Substitution substitution) {
        final List<ResourceKey> options = new ArrayList<>();
        options.add(resource());
        if (substitution.isAllowed() && tag != null) {
            for (NexusResource member : resource().membersOf(tag)) {
                if (!options.contains(member)) {
                    options.add(member);
                }
            }
        }
        return List.copyOf(options);
    }
}
