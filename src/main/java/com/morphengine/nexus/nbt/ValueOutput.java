package com.morphengine.nexus.nbt;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Writes the saved data of a block entity into a compound tag, encoding with
 * the registries of the world. Server thread only.
 */
public final class ValueOutput {

    private final CompoundTag tag;
    private final HolderLookup.Provider registries;

    private ValueOutput(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.tag = Objects.requireNonNull(tag, "tag must not be null");
        this.registries = Objects.requireNonNull(registries, "registries must not be null");
    }

    public static ValueOutput of(final CompoundTag tag, final HolderLookup.Provider registries) {
        return new ValueOutput(tag, registries);
    }

    public CompoundTag tag() {
        return tag;
    }

    public HolderLookup.Provider registries() {
        return registries;
    }

    public void putInt(final String name, final int value) {
        tag.putInt(name, value);
    }

    public void putLong(final String name, final long value) {
        tag.putLong(name, value);
    }

    public void putBoolean(final String name, final boolean value) {
        tag.putBoolean(name, value);
    }

    public void putString(final String name, final String value) {
        tag.putString(name, value);
    }

    public void putFloat(final String name, final float value) {
        tag.putFloat(name, value);
    }

    public <T> void store(final String name, final Codec<T> codec, final T value) {
        tag.put(name, codec.encodeStart(ops(), value).getOrThrow());
    }

    public <T> void storeNullable(final String name, final Codec<T> codec, final @Nullable T value) {
        if (value == null) {
            tag.remove(name);
        } else {
            store(name, codec, value);
        }
    }

    public ValueOutput child(final String name) {
        final CompoundTag child = new CompoundTag();
        tag.put(name, child);
        return new ValueOutput(child, registries);
    }

    public void discard(final String name) {
        tag.remove(name);
    }

    public <T> TypedList<T> list(final String name, final Codec<T> codec) {
        final ListTag list = new ListTag();
        tag.put(name, list);
        return new TypedList<>(list, codec, ops());
    }

    /**
     * Saves the stacks as the item list of the child {@code name}.
     */
    public void saveItems(final String name, final NonNullList<ItemStack> items) {
        child(name).saveItems(items);
    }

    /**
     * Saves the stacks as the item list of this tag.
     */
    public void saveItems(final NonNullList<ItemStack> items) {
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    private RegistryOps<Tag> ops() {
        return registries.createSerializationContext(NbtOps.INSTANCE);
    }

    /**
     * A list that is filled one encoded value at a time.
     */
    public static final class TypedList<T> {

        private final ListTag list;
        private final Codec<T> codec;
        private final RegistryOps<Tag> ops;

        private TypedList(final ListTag list, final Codec<T> codec, final RegistryOps<Tag> ops) {
            this.list = list;
            this.codec = codec;
            this.ops = ops;
        }

        public void add(final T value) {
            list.add(codec.encodeStart(ops, value).getOrThrow());
        }
    }
}
