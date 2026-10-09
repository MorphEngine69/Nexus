package com.morphengine.nexus.resource;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.api.resource.FilterMatchMode;
import com.morphengine.nexus.api.resource.ResourceGroup;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.transfer.ItemResource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public ResourceKey normalized(final FilterMatchMode mode) {
        return switch (mode) {
            case EXACT -> this;
            case IGNORE_DURABILITY -> new ItemKey(item.without(DataComponents.DAMAGE));
            case IGNORE_COMPONENTS -> new ItemKey(ItemResource.of(item.getItem()));
        };
    }

    @Override
    public Component name() {
        return item.getHoverName();
    }

    @Override
    public ResourceLocation id() {
        return BuiltInRegistries.ITEM.getKey(item.getItem());
    }

    @Override
    public List<ResourceLocation> tags() {
        return ResourceTags.tagsOf(BuiltInRegistries.ITEM.wrapAsHolder(item.getItem()));
    }

    @Override
    public List<NexusResource> membersOf(final ResourceLocation tag) {
        return ResourceTags.membersOf(BuiltInRegistries.ITEM, tag, entry ->
                new ItemKey(ItemResource.of(entry.value())));
    }

    @Override
    public Optional<ResourceGroup> tagGroup(final ResourceLocation tag) {
        return Optional.of(new ItemTagGroup(TagKey.create(Registries.ITEM, tag)));
    }
}
