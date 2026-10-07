package com.morphengine.nexus.client.integration;

import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.networking.CraftingGridFillPayload;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.terminal.GridFill;
import com.morphengine.nexus.terminal.TerminalContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A recipe a recipe viewer asks to lay out on the grid of a Crafting Terminal:
 * for each of the nine grid slots, the items that fit it. It tells which slots
 * neither the player nor the network can fill, and sends the recipe to the
 * server, which takes the items from the inventory first, then from the
 * network. Client thread only.
 */
public final class CraftingGridTransfer {

    private final List<List<ItemKey>> slots;

    /**
     * @param slotOptions for each grid slot in reading order, the stacks that fit
     *                    it; empty stacks are skipped, an empty list leaves the slot empty
     */
    public CraftingGridTransfer(final List<? extends List<ItemStack>> slotOptions) {
        final List<List<ItemKey>> keys = new ArrayList<>(slotOptions.size());
        for (List<ItemStack> options : slotOptions) {
            keys.add(options.stream()
                    .filter(stack -> !stack.isEmpty())
                    .map(ItemKey::of)
                    .distinct()
                    .limit(CraftingGridFillPayload.MOST_OPTIONS)
                    .toList());
        }
        this.slots = List.copyOf(keys);
    }

    /**
     * Counts one of an ingredient per slot against what the inventory, the grid
     * and the network hold, so two slots cannot both claim the last item.
     *
     * @return indices of the grid slots whose ingredient nobody has
     */
    public List<Integer> missingSlots(final CraftingTerminalMenu menu) {
        final TerminalContents network = menu.terminal().contents();
        final Map<ItemKey, Long> stock = new HashMap<>();
        final List<Integer> missing = new ArrayList<>();
        for (int slot = 0; slot < slots.size(); slot++) {
            final List<ItemKey> options = slots.get(slot);
            if (!options.isEmpty() && !takeOne(options, stock, menu, network)) {
                missing.add(slot);
            }
        }
        return missing;
    }

    private static boolean takeOne(final List<ItemKey> options, final Map<ItemKey, Long> stock,
                                   final CraftingTerminalMenu menu, final TerminalContents network) {
        for (ItemKey option : options) {
            final long left = stock.computeIfAbsent(option, key -> held(menu, key) + network.amountOf(key));
            if (left > 0) {
                stock.put(option, left - 1);
                return true;
            }
        }
        return false;
    }

    /**
     * @return how many of {@code item} the player's inventory and the crafting grid hold
     */
    private static long held(final CraftingTerminalMenu menu, final ItemKey item) {
        long count = 0;
        for (ItemStack stack : menu.ingredientsAtHand()) {
            if (item.item().matches(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public void send(final CraftingTerminalMenu menu, final GridFill amount) {
        ClientPacketDistributor.sendToServer(new CraftingGridFillPayload(menu.containerId, slots, amount));
    }
}
