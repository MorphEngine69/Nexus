package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.blueprint.EncodedBlueprint;
import com.morphengine.nexus.blueprint.ProcessingBlueprint;
import com.morphengine.nexus.item.BlueprintItem;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The blueprint slots of an Assembler: one encoded Blueprint in a slot. What
 * they encode is read once after every change.
 */
public final class BlueprintSlots extends SimpleContainer {

    public static final int SIZE = 9;

    private final Runnable onChange;
    private Map<Blueprint, EncodedBlueprint> encoded = Map.of();

    /**
     * @param onChange run after every change of the slots
     */
    public BlueprintSlots(final Runnable onChange) {
        super(SIZE);
        this.onChange = Objects.requireNonNull(onChange, "onChange must not be null");
    }

    /**
     * @return what the slots encode, each blueprint once, in slot order
     */
    public List<Blueprint> blueprints() {
        return List.copyOf(encoded.keySet());
    }

    /**
     * @return the encoding of {@code blueprint} held here; {@code null} when no slot holds it
     */
    public @Nullable EncodedBlueprint encodingOf(final Blueprint blueprint) {
        return encoded.get(blueprint);
    }

    /**
     * @return the processing blueprints held, whose outputs come back from the machine
     */
    public List<Blueprint> processing() {
        final List<Blueprint> processing = new ArrayList<>();
        for (Map.Entry<Blueprint, EncodedBlueprint> held : encoded.entrySet()) {
            if (held.getValue() instanceof ProcessingBlueprint) {
                processing.add(held.getKey());
            }
        }
        return processing;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return BlueprintItem.encodedOn(stack) != null;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        readBlueprints();
        onChange.run();
    }

    /**
     * Reads what the slots encode, after they were filled without {@link #setChanged}.
     */
    public void readBlueprints() {
        final Map<Blueprint, EncodedBlueprint> read = new LinkedHashMap<>();
        for (ItemStack stack : getItems()) {
            final EncodedBlueprint blueprint = BlueprintItem.encodedOn(stack);
            if (blueprint != null) {
                read.putIfAbsent(blueprint.blueprint(), blueprint);
            }
        }
        encoded = read;
    }
}
