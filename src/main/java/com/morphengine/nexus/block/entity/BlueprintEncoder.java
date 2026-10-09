package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.blueprint.BlueprintDraft;
import com.morphengine.nexus.blueprint.EncodedBlueprint;
import com.morphengine.nexus.blueprint.GridCrafting;
import com.morphengine.nexus.blueprint.GridRecipe;
import com.morphengine.nexus.item.BlueprintItem;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.registry.NexusDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * What a Blueprint Terminal encodes with: the draft set in its ghost slots, a
 * slot of blank Blueprints and a slot the encoded one comes out of. An
 * encoded Blueprint put in that slot loads into the draft, to be changed and
 * encoded over. The terminal keeps it, so it stays when the panel closes.
 */
public final class BlueprintEncoder {

    public static final int BLANK_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private static final String TAG_DRAFT = "draft";
    private static final String TAG_BLUEPRINTS = "blueprints";

    private final Runnable onChange;
    private final SimpleContainer blueprints = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            onChange.run();
        }
    };
    private BlueprintDraft draft = BlueprintDraft.EMPTY;

    /**
     * @param onChange run after every change of the draft or the slots
     */
    public BlueprintEncoder(final Runnable onChange) {
        this.onChange = Objects.requireNonNull(onChange, "onChange must not be null");
    }

    public BlueprintDraft draft() {
        return draft;
    }

    public void changeDraft(final BlueprintDraft changed) {
        draft = Objects.requireNonNull(changed, "changed must not be null");
        onChange.run();
    }

    public SimpleContainer blueprints() {
        return blueprints;
    }

    /**
     * Makes the draft that of {@code stack}, when it is an encoded Blueprint.
     */
    public void loadFrom(final ItemStack stack) {
        final EncodedBlueprint encoded = BlueprintItem.encodedOn(stack);
        if (encoded != null) {
            changeDraft(BlueprintDraft.of(encoded));
        }
    }

    /**
     * @return what the crafting grid of the draft crafts; nothing for a processing draft
     */
    public List<ResourceAmount> craftingOutputs(final ServerLevel level) {
        return draft.kind() == BlueprintKind.CRAFTING ? GridCrafting.outputsOf(level, draft.grid()) : List.of();
    }

    /**
     * Encodes the draft onto the Blueprint in the output slot, or onto one taken
     * from the blank slot when the output slot is empty.
     *
     * @return whether a Blueprint was encoded; not when the draft is incomplete
     *         or there is no Blueprint to encode onto
     */
    public boolean encode(final ServerLevel level) {
        final EncodedBlueprint encoded = draft.encode(draft.kind() == BlueprintKind.CRAFTING
                ? GridCrafting.recipeOf(level, draft.grid()) : GridRecipe.NONE);
        final ItemStack target = encoded != null ? takeTarget() : null;
        if (encoded == null || target == null) {
            return false;
        }
        target.set(NexusDataComponents.ENCODED_BLUEPRINT.get(), encoded);
        blueprints.setItem(OUTPUT_SLOT, target);
        return true;
    }

    private @Nullable ItemStack takeTarget() {
        final ItemStack output = blueprints.getItem(OUTPUT_SLOT);
        if (output.getItem() instanceof BlueprintItem && output.getCount() == 1) {
            return output.copy();
        }
        if (!output.isEmpty() || !BlueprintItem.isBlank(blueprints.getItem(BLANK_SLOT))) {
            return null;
        }
        return blueprints.removeItem(BLANK_SLOT, 1);
    }

    public void write(final ValueOutput output) {
        output.store(TAG_DRAFT, BlueprintDraft.CODEC, draft);
        output.saveItems(TAG_BLUEPRINTS, blueprints.getItems());
    }

    public void read(final ValueInput input) {
        draft = input.read(TAG_DRAFT, BlueprintDraft.CODEC).orElse(BlueprintDraft.EMPTY);
        input.loadItems(TAG_BLUEPRINTS, blueprints.getItems());
    }
}
