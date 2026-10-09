package com.morphengine.nexus.client.integration;

import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.blueprint.BlueprintDraft;
import com.morphengine.nexus.blueprint.Substitution;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.networking.BlueprintRecipePayload;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.terminal.TerminalContents;
import com.morphengine.nexus.transfer.FluidResource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe a recipe viewer asks to lay out as the draft of a Blueprint
 * Terminal: a crafting recipe on the grid, taking substitutes, since the
 * result does not depend on them; any other recipe as processing, with its
 * inputs and outputs in the order the viewer shows them and without
 * substitutes, as a machine may give something else for another input.
 * Client thread only.
 */
public final class BlueprintRecipeTransfer {

    private final BlueprintDraft draft;

    private BlueprintRecipeTransfer(final BlueprintDraft draft) {
        this.draft = draft;
    }

    /**
     * @param grid    for each of the nine grid slots in reading order, the stacks
     *                that fit it; an empty list leaves the slot empty
     * @param network what the network holds; of several stacks that fit a
     *                slot, the first it holds is picked, otherwise the first
     */
    public static BlueprintRecipeTransfer crafting(final List<? extends List<ItemStack>> grid,
                                                   final TerminalContents network) {
        final List<BlueprintDraft.Slot> slots = new ArrayList<>();
        for (int index = 0; index < Math.min(grid.size(), BlueprintDraft.INPUTS); index++) {
            final ItemKey picked = pick(grid.get(index), network);
            if (picked != null) {
                slots.add(BlueprintDraft.slotOf(BlueprintKind.CRAFTING, index, picked, 1));
            }
        }
        return new BlueprintRecipeTransfer(new BlueprintDraft(BlueprintKind.CRAFTING, slots, Substitution.ALLOWED));
    }

    /**
     * @param inputs  what the recipe takes, in the order shown; items and fluids alike
     * @param outputs what it gives, the main product first
     */
    public static BlueprintRecipeTransfer processing(final List<ResourceAmount> inputs,
                                                     final List<ResourceAmount> outputs) {
        final List<BlueprintDraft.Slot> slots = new ArrayList<>();
        addSlots(slots, 0, BlueprintDraft.INPUTS, inputs);
        addSlots(slots, BlueprintDraft.INPUTS, BlueprintDraft.OUTPUTS, outputs);
        return new BlueprintRecipeTransfer(new BlueprintDraft(BlueprintKind.PROCESSING, slots, Substitution.EXACT));
    }

    /**
     * @return {@code stack} as a resource with its count; {@code null} for an empty stack
     */
    public static @Nullable ResourceAmount amountOf(final ItemStack stack) {
        return stack.isEmpty() ? null : new ResourceAmount(ItemKey.of(stack), stack.getCount());
    }

    /**
     * @return {@code fluid} as a resource in millibuckets; {@code null} for an empty stack
     */
    public static @Nullable ResourceAmount amountOf(final FluidStack fluid) {
        return fluid.isEmpty() ? null : new ResourceAmount(new FluidKey(FluidResource.of(fluid)), fluid.getAmount());
    }

    /**
     * @return whether the recipe gives the draft anything to encode
     */
    public boolean isEmpty() {
        return draft.slots().isEmpty();
    }

    public void send(final BlueprintTerminalMenu menu) {
        PacketDistributor.sendToServer(new BlueprintRecipePayload(menu.containerId, draft));
    }

    private static @Nullable ItemKey pick(final List<ItemStack> options, final TerminalContents network) {
        ItemKey first = null;
        for (ItemStack option : options) {
            if (option.isEmpty()) {
                continue;
            }
            final ItemKey key = ItemKey.of(option);
            if (network.amountOf(key) > 0) {
                return key;
            }
            first = first != null ? first : key;
        }
        return first;
    }

    private static void addSlots(final List<BlueprintDraft.Slot> slots, final int first, final int count,
                                 final List<ResourceAmount> amounts) {
        for (int i = 0; i < Math.min(count, amounts.size()); i++) {
            final ResourceAmount amount = amounts.get(i);
            final NexusResource resource = NexusResources.of(amount.resource());
            slots.add(BlueprintDraft.slotOf(BlueprintKind.PROCESSING, first + i, resource,
                    Math.min(amount.amount(), BlueprintDraft.MAX_AMOUNT)));
        }
    }
}
