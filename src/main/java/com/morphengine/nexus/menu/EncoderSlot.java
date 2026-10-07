package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.BlueprintEncoder;
import com.morphengine.nexus.item.BlueprintItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A slot of a Blueprint Terminal's encoder. The blank slot takes blank
 * Blueprints only; the output slot takes any Blueprint, and one the player
 * puts in loads into the draft.
 */
final class EncoderSlot extends Slot {

    private final @Nullable BlueprintEncoder encoder;

    /**
     * @param encoder the encoder to load an encoded Blueprint into; {@code null} on the client
     */
    EncoderSlot(final Container container, final int index, final @Nullable BlueprintEncoder encoder) {
        super(container, index, 0, 0);
        this.encoder = encoder;
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return getContainerSlot() == BlueprintEncoder.BLANK_SLOT
                ? BlueprintItem.isBlank(stack) : stack.getItem() instanceof BlueprintItem;
    }

    @Override
    public int getMaxStackSize() {
        return getContainerSlot() == BlueprintEncoder.OUTPUT_SLOT ? 1 : super.getMaxStackSize();
    }

    @Override
    public void setByPlayer(final ItemStack newStack, final ItemStack oldStack) {
        super.setByPlayer(newStack, oldStack);
        if (encoder != null && getContainerSlot() == BlueprintEncoder.OUTPUT_SLOT) {
            encoder.loadFrom(newStack);
        }
    }
}
