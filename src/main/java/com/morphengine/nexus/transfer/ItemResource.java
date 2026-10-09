package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * An item with its components, without a count: what an item handler or a stack is made of, and the key of an item
 * in a network. Immutable.
 */
public final class ItemResource {

    public static final ItemResource EMPTY = new ItemResource(ItemStack.EMPTY);
    public static final Codec<ItemResource> CODEC =
            ItemStack.SINGLE_ITEM_CODEC.xmap(ItemResource::of, resource -> resource.toStack(1));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemResource> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(ItemResource::of, resource -> resource.toStack(1));

    private final ItemStack template;

    private ItemResource(final ItemStack template) {
        this.template = template;
    }

    public static ItemResource of(final ItemStack stack) {
        Objects.requireNonNull(stack, "stack must not be null");
        return stack.isEmpty() ? EMPTY : new ItemResource(stack.copyWithCount(1));
    }

    public static ItemResource of(final Item item) {
        Objects.requireNonNull(item, "item must not be null");
        return of(new ItemStack(item));
    }

    public boolean isEmpty() {
        return template.isEmpty();
    }

    public Item getItem() {
        return template.getItem();
    }

    public int getMaxStackSize() {
        return template.getMaxStackSize();
    }

    public Component getHoverName() {
        return template.getHoverName();
    }

    public ItemStack toStack(final int count) {
        return template.copyWithCount(count);
    }

    public boolean matches(final ItemStack stack) {
        return ItemStack.isSameItemSameComponents(template, stack);
    }

    public ItemResource without(final DataComponentType<?> type) {
        final ItemStack copy = template.copy();
        copy.remove(type);
        return of(copy);
    }

    public boolean is(final TagKey<Item> tag) {
        return template.is(tag);
    }

    public boolean isDamaged() {
        return template.has(DataComponents.DAMAGE) && template.getDamageValue() > 0;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return this == other
                || other instanceof ItemResource resource && ItemStack.isSameItemSameComponents(template,
                        resource.template);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(template);
    }

    @Override
    public String toString() {
        return "ItemResource[" + template + "]";
    }
}
