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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads the saved data of a block entity from a compound tag, decoding with
 * the registries of the world. A missing or malformed entry reads as absent.
 * Server thread only.
 */
public final class ValueInput {

    private static final int COMPOUND_TAG = 10;

    private final CompoundTag tag;
    private final HolderLookup.Provider registries;

    private ValueInput(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.tag = Objects.requireNonNull(tag, "tag must not be null");
        this.registries = Objects.requireNonNull(registries, "registries must not be null");
    }

    public static ValueInput of(final CompoundTag tag, final HolderLookup.Provider registries) {
        return new ValueInput(tag, registries);
    }

    public CompoundTag tag() {
        return tag;
    }

    public HolderLookup.Provider registries() {
        return registries;
    }

    public int getIntOr(final String name, final int defaultValue) {
        return tag.contains(name) ? tag.getInt(name) : defaultValue;
    }

    public long getLongOr(final String name, final long defaultValue) {
        return tag.contains(name) ? tag.getLong(name) : defaultValue;
    }

    public boolean getBooleanOr(final String name, final boolean defaultValue) {
        return tag.contains(name) ? tag.getBoolean(name) : defaultValue;
    }

    public float getFloatOr(final String name, final float defaultValue) {
        return tag.contains(name) ? tag.getFloat(name) : defaultValue;
    }

    public String getStringOr(final String name, final String defaultValue) {
        return tag.contains(name) ? tag.getString(name) : defaultValue;
    }

    public <T> Optional<T> read(final String name, final Codec<T> codec) {
        final Tag value = tag.get(name);
        return value == null ? Optional.empty() : codec.parse(ops(), value).result();
    }

    public ValueInput childOrEmpty(final String name) {
        final CompoundTag child = tag.contains(name, COMPOUND_TAG) ? tag.getCompound(name) : new CompoundTag();
        return new ValueInput(child, registries);
    }

    public <T> List<T> listOrEmpty(final String name, final Codec<T> codec) {
        final List<T> values = new ArrayList<>();
        if (tag.get(name) instanceof ListTag list) {
            for (Tag element : list) {
                codec.parse(ops(), element).result().ifPresent(values::add);
            }
        }
        return values;
    }

    /**
     * Loads the item list of the child {@code name} into {@code items}.
     */
    public void loadItems(final String name, final NonNullList<ItemStack> items) {
        childOrEmpty(name).loadItems(items);
    }

    /**
     * Loads the item list of this tag into {@code items}.
     */
    public void loadItems(final NonNullList<ItemStack> items) {
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    private RegistryOps<Tag> ops() {
        return registries.createSerializationContext(NbtOps.INSTANCE);
    }
}
