package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The name a player gave a device from the title of its panel. Kept in the same
 * tag and item component as a name given on an anvil, so it moves to the
 * dropped item and back onto the placed block.
 */
final class DeviceName {

    /** Same key vanilla block entities use, so an anvil-named item reads the same way. */
    private static final String TAG = "CustomName";

    private @Nullable Component custom;

    /**
     * @return the player's name, or {@code fallback} when the device has none
     */
    Component orDefault(final Component fallback) {
        return custom != null ? custom : fallback;
    }

    /**
     * @param name the new name; blank restores the default name
     */
    void rename(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        final String stripped = name.strip();
        if (stripped.length() > Renamable.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "device name longer than " + Renamable.MAX_NAME_LENGTH + " characters: " + stripped);
        }
        custom = stripped.isEmpty() ? null : Component.literal(stripped);
    }

    void save(final ValueOutput output) {
        output.storeNullable(TAG, ComponentSerialization.CODEC, custom);
    }

    void load(final ValueInput input) {
        custom = input.read(TAG, ComponentSerialization.CODEC).orElse(null);
    }

    void applyFrom(final ComponentSource components) {
        custom = components.get(DataComponents.CUSTOM_NAME);
    }

    void collectInto(final DataComponentMap.Builder components) {
        components.set(DataComponents.CUSTOM_NAME, custom);
    }

    static void removeFrom(final CompoundTag tag) {
        tag.remove(TAG);
    }
}
