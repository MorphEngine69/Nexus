package com.morphengine.nexus.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.transfer.ItemResource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One filled slot of a crafting grid.
 *
 * @param slot     index in the 3 by 3 grid, row by row, from 0 to 8
 * @param item     the item encoded in the slot
 * @param accepted what the recipe accepts in the slot, such as any planks;
 *                 {@code null} when it was not worked out, and then the slot
 *                 takes nothing but {@code item}
 */
public record GridSlot(int slot, ItemKey item, @Nullable Ingredient accepted) {

    public static final int SIDE = 3;
    public static final int COUNT = SIDE * SIDE;

    public static final Codec<GridSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(0, COUNT - 1).fieldOf("slot").forGetter(GridSlot::slot),
                    ItemKey.CODEC.forGetter(GridSlot::item),
                    Ingredient.CODEC.optionalFieldOf("accepts").forGetter(GridSlot::acceptedIfAny))
            .apply(instance, GridSlot::of));

    public static final StreamCodec<RegistryFriendlyByteBuf, GridSlot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GridSlot::slot,
            ItemKey.STREAM_CODEC, GridSlot::item,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), GridSlot::acceptedIfAny,
            GridSlot::of);

    public GridSlot {
        Objects.requireNonNull(item, "item must not be null");
        if (slot < 0 || slot >= COUNT) {
            throw new IllegalArgumentException("grid slot of " + item + " out of range: " + slot);
        }
    }

    /**
     * A slot that takes nothing but {@code item}.
     */
    public GridSlot(final int slot, final ItemKey item) {
        this(slot, item, null);
    }

    private static GridSlot of(final int slot, final ItemKey item, final Optional<Ingredient> accepted) {
        return new GridSlot(slot, item, accepted.orElse(null));
    }

    private Optional<Ingredient> acceptedIfAny() {
        return Optional.ofNullable(accepted);
    }

    /**
     * @return the items the slot takes: its own item, then, when substitutes
     *         are allowed, every other item the recipe accepts in it
     */
    public List<ResourceKey> options(final Substitution substitution) {
        final List<ResourceKey> options = new ArrayList<>();
        options.add(item);
        if (substitution.isAllowed() && accepted != null) {
            for (ItemStack entry : accepted.getItems()) {
                final ItemKey other = new ItemKey(ItemResource.of(entry.getItem()));
                if (!options.contains(other)) {
                    options.add(other);
                }
            }
        }
        return List.copyOf(options);
    }
}
