package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.level.PlayerActor;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.terminal.GridFill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills the grid of a Crafting Terminal with a recipe sent by a recipe viewer
 * such as JEI, REI or EMI. Every slot lists the items that fit it; each slot
 * gets the first of them at hand, taken from the player's inventory first and
 * from the network for whatever the inventory lacks. What was on the grid goes
 * back into the network, or into the inventory if the network is full.
 * Server thread only.
 */
final class CraftingGridFiller {

    private final ServerPlayer player;
    private final CraftingContainer grid;
    private final @Nullable Storage storage;
    private final Actor actor;

    /**
     * @param storage the network's storage; {@code null} while the terminal is offline
     */
    CraftingGridFiller(final ServerPlayer player, final CraftingContainer grid, final @Nullable Storage storage) {
        this.player = player;
        this.grid = grid;
        this.storage = storage;
        this.actor = PlayerActor.of(player);
    }

    /**
     * Picks for every slot the first item that fits it and is still at hand,
     * then puts the same number into each slot: one for a single craft, or as
     * many crafts as the inventory and the network hold ingredients for, up to
     * a stack of the smallest stacking ingredient.
     *
     * @param slots for each grid slot, the items that fit it in order of preference;
     *              an empty list leaves the slot empty
     */
    void fill(final List<List<ItemKey>> slots, final GridFill amount) {
        returnGrid();
        final Map<ItemKey, Long> stock = new HashMap<>();
        final List<@Nullable ItemKey> chosen = new ArrayList<>();
        for (int slot = 0; slot < Math.min(slots.size(), grid.getContainerSize()); slot++) {
            chosen.add(chooseOne(slots.get(slot), stock));
        }
        final int crafts = amount == GridFill.ONE_CRAFT ? 1 : craftsAtHand(chosen);
        for (int slot = 0; slot < chosen.size(); slot++) {
            final ItemKey item = chosen.get(slot);
            if (item != null) {
                grid.setItem(slot, take(item, crafts));
            }
        }
    }

    /**
     * @return the first option of which one is left in {@code stock}, counted off it;
     *         {@code null} when none is
     */
    private @Nullable ItemKey chooseOne(final List<ItemKey> options, final Map<ItemKey, Long> stock) {
        for (ItemKey option : options) {
            final long left = stock.computeIfAbsent(option, this::atHand);
            if (left > 0) {
                stock.put(option, left - 1);
                return option;
            }
        }
        return null;
    }

    /**
     * @return how many crafts the chosen items last for; at least one
     */
    private int craftsAtHand(final List<@Nullable ItemKey> chosen) {
        final Map<ItemKey, Integer> perCraft = new HashMap<>();
        for (ItemKey item : chosen) {
            if (item != null) {
                perCraft.merge(item, 1, Integer::sum);
            }
        }
        long crafts = Long.MAX_VALUE;
        for (Map.Entry<ItemKey, Integer> use : perCraft.entrySet()) {
            crafts = Math.min(crafts, Math.min(use.getKey().maxStackSize(), atHand(use.getKey()) / use.getValue()));
        }
        return (int) Math.max(1, Math.min(crafts, Integer.MAX_VALUE));
    }

    /**
     * @return how many of {@code item} the player's inventory and the network hold
     */
    private long atHand(final ItemKey item) {
        final Inventory inventory = player.getInventory();
        long count = 0;
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (item.item().matches(stack)) {
                count += stack.getCount();
            }
        }
        if (storage != null) {
            count += storage.extract(item, Integer.MAX_VALUE, Action.SIMULATE, actor);
        }
        return count;
    }

    /**
     * Empties the grid into the network, or into the inventory for what the
     * network does not take.
     */
    void returnGrid() {
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            final ItemStack stack = grid.removeItemNoUpdate(slot);
            if (storage != null && !stack.isEmpty()) {
                stack.shrink((int) storage.insert(ItemKey.of(stack), stack.getCount(), Action.EXECUTE, actor));
            }
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
            }
        }
        grid.setChanged();
    }

    /**
     * @return up to {@code count} of {@code item}, from the inventory first, then the network
     */
    private ItemStack take(final ItemKey item, final int count) {
        final Inventory inventory = player.getInventory();
        int missing = count;
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size() && missing > 0; slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (item.item().matches(stack)) {
                missing -= inventory.removeItem(slot, Math.min(missing, stack.getCount())).getCount();
            }
        }
        if (missing > 0 && storage != null) {
            missing -= (int) storage.extract(item, missing, Action.EXECUTE, actor);
        }
        return count > missing ? item.toStack(count - missing) : ItemStack.EMPTY;
    }
}
