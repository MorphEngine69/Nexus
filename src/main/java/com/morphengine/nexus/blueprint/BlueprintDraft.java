package com.morphengine.nexus.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a Blueprint Terminal is about to encode: slots 0 to 8 are the inputs,
 * a 3 by 3 crafting grid when crafting, and slots 9 to 11 the outputs of
 * processing. A crafting draft holds one item per slot and no outputs of its
 * own; what the grid crafts is worked out from the recipes. A processing input
 * keeps the tag its substitutes come from, which it gets when it is set: the
 * first tag of its resource, shared {@code c:} tags first.
 *
 * @param slots        the filled slots, each once
 * @param substitution whether the Blueprint encoded also takes substitutes
 */
public record BlueprintDraft(BlueprintKind kind, List<Slot> slots, Substitution substitution) {

    public static final int INPUTS = GridSlot.COUNT;
    public static final int OUTPUTS = ProcessingBlueprint.MAX_OUTPUTS;
    public static final int SIZE = INPUTS + OUTPUTS;
    /** The most units a processing slot lists. */
    public static final long MAX_AMOUNT = Integer.MAX_VALUE;
    public static final BlueprintDraft EMPTY =
            new BlueprintDraft(BlueprintKind.CRAFTING, List.of(), Substitution.EXACT);

    public static final Codec<BlueprintDraft> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BlueprintCodecs.KIND_CODEC.fieldOf("kind").forGetter(BlueprintDraft::kind),
                    Slot.CODEC.listOf().fieldOf("slots").forGetter(BlueprintDraft::slots),
                    Substitution.CODEC.optionalFieldOf("substitution", Substitution.EXACT)
                            .forGetter(BlueprintDraft::substitution))
            .apply(instance, BlueprintDraft::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintDraft> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(BlueprintKind.class), BlueprintDraft::kind,
            Slot.STREAM_CODEC.apply(ByteBufCodecs.list(SIZE)), BlueprintDraft::slots,
            Substitution.STREAM_CODEC, BlueprintDraft::substitution,
            BlueprintDraft::new);

    public BlueprintDraft {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(substitution, "substitution must not be null");
        slots = List.copyOf(slots);
        final Set<Integer> filled = new HashSet<>();
        for (Slot slot : slots) {
            if (!filled.add(slot.index())) {
                throw new IllegalArgumentException("draft slot " + slot.index() + " filled twice: " + slots);
            }
        }
    }

    /**
     * @return the draft of {@code encoded}, to change and encode again
     */
    public static BlueprintDraft of(final EncodedBlueprint encoded) {
        final List<Slot> slots = new ArrayList<>();
        final Substitution substitution = switch (encoded) {
            case CraftingBlueprint crafting -> {
                for (GridSlot slot : crafting.grid()) {
                    slots.add(new Slot(slot.slot(), slot.item(), 1, null));
                }
                yield crafting.substitution();
            }
            case ProcessingBlueprint processing -> {
                for (int i = 0; i < processing.inputs().size(); i++) {
                    final ProcessingInput input = processing.inputs().get(i);
                    slots.add(new Slot(i, input.resource(), input.amount().amount(), input.tag()));
                }
                addAll(slots, INPUTS, processing.outputs());
                yield processing.substitution();
            }
        };
        return new BlueprintDraft(encoded.kind(), slots, substitution);
    }

    public @Nullable Slot at(final int index) {
        for (Slot slot : slots) {
            if (slot.index() == index) {
                return slot;
            }
        }
        return null;
    }

    /**
     * @param resource what the slot lists now; {@code null} empties it
     * @return this draft with {@code index} set to one step of {@code resource};
     *         unchanged for a slot out of range, and in a crafting draft for an
     *         output slot or a resource that is not an item
     */
    public BlueprintDraft with(final int index, final @Nullable NexusResource resource) {
        if (!accepts(index, resource)) {
            return this;
        }
        final List<Slot> updated = without(index);
        if (resource != null) {
            updated.add(slotOf(kind, index, resource,
                    kind == BlueprintKind.CRAFTING ? 1 : resource.type().unit().step()));
        }
        return new BlueprintDraft(kind, updated, substitution);
    }

    /**
     * @return slot {@code index} listing {@code amount} of {@code resource}; a
     *         processing input also gets the first tag of the resource
     */
    public static Slot slotOf(final BlueprintKind kind, final int index, final NexusResource resource,
                              final long amount) {
        final boolean hasTag = kind == BlueprintKind.PROCESSING && index < INPUTS;
        return new Slot(index, resource, amount, hasTag ? firstTagOf(resource) : null);
    }

    private static @Nullable Identifier firstTagOf(final NexusResource resource) {
        final List<Identifier> tags = resource.tags();
        return tags.isEmpty() ? null : tags.getFirst();
    }

    /**
     * @param amount units the slot lists; clamped to [1, {@value #MAX_AMOUNT}]
     * @return this draft with the amount of {@code index} changed; unchanged
     *         for an empty slot and in a crafting draft, whose slots hold one item
     */
    public BlueprintDraft withAmount(final int index, final long amount) {
        final Slot slot = at(index);
        if (slot == null || kind == BlueprintKind.CRAFTING) {
            return this;
        }
        final List<Slot> updated = without(index);
        updated.add(new Slot(index, slot.resource(), Math.clamp(amount, 1, MAX_AMOUNT), slot.tag()));
        return new BlueprintDraft(kind, updated, substitution);
    }

    /**
     * Moves a processing input on to the next tag of its resource, and from the
     * last one to none, which takes nothing but the resource itself.
     *
     * @param step one to go forwards, minus one to go backwards
     * @return this draft with the tag of {@code index} changed; unchanged for an
     *         empty slot, an output, and in a crafting draft, whose substitutes
     *         come from the recipe
     */
    public BlueprintDraft withNextTag(final int index, final int step) {
        final Slot slot = at(index);
        if (slot == null || kind == BlueprintKind.CRAFTING || index >= INPUTS) {
            return this;
        }
        final List<@Nullable Identifier> choices = new ArrayList<>(slot.resource().tags());
        choices.add(null);
        final int current = choices.indexOf(slot.tag());
        final Identifier next = choices.get(Math.floorMod(current + step, choices.size()));
        final List<Slot> updated = without(index);
        updated.add(new Slot(index, slot.resource(), slot.amount(), next));
        return new BlueprintDraft(kind, updated, substitution);
    }

    public BlueprintDraft withSubstitution(final Substitution changed) {
        return new BlueprintDraft(kind, slots, changed);
    }

    /**
     * @return this draft switched to {@code newKind}; switching to crafting keeps
     *         only the items of the inputs, one of each slot
     */
    public BlueprintDraft withKind(final BlueprintKind newKind) {
        final List<Slot> kept = new ArrayList<>();
        for (Slot slot : slots) {
            if (newKind == BlueprintKind.PROCESSING) {
                kept.add(kind == BlueprintKind.CRAFTING
                        ? slotOf(newKind, slot.index(), slot.resource(), slot.amount()) : slot);
            } else if (slot.index() < INPUTS && slot.resource() instanceof ItemKey) {
                kept.add(new Slot(slot.index(), slot.resource(), 1, null));
            }
        }
        return new BlueprintDraft(newKind, kept, substitution);
    }

    public BlueprintDraft cleared() {
        return new BlueprintDraft(kind, List.of(), substitution);
    }

    /**
     * @return the crafting grid, one stack per input slot; empty where nothing lies
     */
    public List<ItemStack> grid() {
        final NonNullList<ItemStack> grid = NonNullList.withSize(INPUTS, ItemStack.EMPTY);
        for (Slot slot : slots) {
            if (slot.index() < INPUTS && slot.resource() instanceof ItemKey item) {
                grid.set(slot.index(), item.toStack(1));
            }
        }
        return grid;
    }

    /**
     * @param recipe what the grid crafts and what its recipe accepts in each
     *               slot, for a crafting draft; ignored for processing
     * @return the blueprint this draft encodes; {@code null} while it lacks
     *         an input or an output
     */
    public @Nullable EncodedBlueprint encode(final GridRecipe recipe) {
        if (kind == BlueprintKind.CRAFTING) {
            final List<GridSlot> grid = new ArrayList<>();
            for (Slot slot : inOrder()) {
                if (slot.index() < INPUTS && slot.resource() instanceof ItemKey item) {
                    grid.add(new GridSlot(slot.index(), item, recipe.accepted().get(slot.index())));
                }
            }
            return grid.isEmpty() || recipe.outputs().isEmpty() ? null
                    : new CraftingBlueprint(grid, recipe.outputs(), substitution);
        }
        final List<ProcessingInput> inputs = new ArrayList<>();
        for (Slot slot : inOrder()) {
            if (slot.index() < INPUTS) {
                inputs.add(new ProcessingInput(new ResourceAmount(slot.resource(), slot.amount()), slot.tag()));
            }
        }
        final List<ResourceAmount> outputs = amountsIn(INPUTS, SIZE);
        return inputs.isEmpty() || outputs.isEmpty() ? null
                : new ProcessingBlueprint(inputs, outputs, substitution);
    }

    private boolean accepts(final int index, final @Nullable NexusResource resource) {
        if (index < 0 || index >= SIZE) {
            return false;
        }
        return kind == BlueprintKind.PROCESSING || index < INPUTS && (resource == null || resource instanceof ItemKey);
    }

    private List<Slot> without(final int index) {
        final List<Slot> kept = new ArrayList<>(slots.size() + 1);
        for (Slot slot : slots) {
            if (slot.index() != index) {
                kept.add(slot);
            }
        }
        return kept;
    }

    private List<Slot> inOrder() {
        final List<Slot> sorted = new ArrayList<>(slots);
        sorted.sort((first, second) -> Integer.compare(first.index(), second.index()));
        return sorted;
    }

    private List<ResourceAmount> amountsIn(final int from, final int to) {
        final List<ResourceAmount> amounts = new ArrayList<>();
        for (Slot slot : inOrder()) {
            if (slot.index() >= from && slot.index() < to) {
                amounts.add(new ResourceAmount(slot.resource(), slot.amount()));
            }
        }
        return amounts;
    }

    private static void addAll(final List<Slot> slots, final int first, final List<ResourceAmount> amounts) {
        for (int i = 0; i < amounts.size(); i++) {
            final ResourceAmount amount = amounts.get(i);
            slots.add(new Slot(first + i, NexusResources.of(amount.resource()), amount.amount(), null));
        }
    }

    /**
     * @param index  slot of the draft, from 0 to {@value BlueprintDraft#SIZE} exclusive
     * @param amount units listed, from 1 to {@value BlueprintDraft#MAX_AMOUNT}
     * @param tag    for a processing input, the tag its substitutes come from;
     *               {@code null} when it has none, and always for other slots
     */
    public record Slot(int index, NexusResource resource, long amount, @Nullable Identifier tag) {

        static final Codec<Slot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.intRange(0, SIZE - 1).fieldOf("slot").forGetter(Slot::index),
                        NexusResources.CODEC.fieldOf("resource").forGetter(Slot::resource),
                        Codec.LONG.fieldOf("amount").forGetter(Slot::amount),
                        Identifier.CODEC.optionalFieldOf("tag").forGetter(Slot::tagIfAny))
                .apply(instance, (index, resource, amount, tag) -> new Slot(index, resource, amount,
                        tag.orElse(null))));

        static final StreamCodec<RegistryFriendlyByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Slot::index,
                NexusResources.STREAM_CODEC, Slot::resource,
                ByteBufCodecs.VAR_LONG, Slot::amount,
                ByteBufCodecs.optional(Identifier.STREAM_CODEC), Slot::tagIfAny,
                (index, resource, amount, tag) -> new Slot(index, resource, amount, tag.orElse(null)));

        public Slot {
            Objects.requireNonNull(resource, "resource must not be null");
            if (index < 0 || index >= SIZE || amount < 1 || amount > MAX_AMOUNT) {
                throw new IllegalArgumentException("draft slot out of range: slot " + index + ", " + amount
                        + " of " + resource);
            }
        }

        private Optional<Identifier> tagIfAny() {
            return Optional.ofNullable(tag);
        }
    }
}
