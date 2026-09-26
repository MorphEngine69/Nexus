package com.morphengine.nexus.resource;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Objects;

/**
 * An item with its components, never empty.
 */
public record ItemKey(ItemResource item) implements NexusResource {

    public static final MapCodec<ItemKey> CODEC = ItemResource.CODEC.xmap(ItemKey::new, ItemKey::item).fieldOf("item");
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemKey> STREAM_CODEC =
            ItemResource.STREAM_CODEC.map(ItemKey::new, ItemKey::item);

    public ItemKey {
        Objects.requireNonNull(item, "item must not be null");
        if (item.isEmpty()) {
            throw new IllegalArgumentException("item resource must not be empty");
        }
    }

    public static ItemKey of(final ItemStack stack) {
        return new ItemKey(ItemResource.of(stack));
    }

    public ItemStack toStack(final int count) {
        return item.toStack(count);
    }

    public int maxStackSize() {
        return item.getMaxStackSize();
    }

    @Override
    public NexusResourceType<?> type() {
        return ResourceTypes.ITEM.get();
    }

    @Override
    public Component name() {
        return item.getHoverName();
    }

    @Override
    public Identifier id() {
        return BuiltInRegistries.ITEM.getKey(item.getItem());
    }
}
