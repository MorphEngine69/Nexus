package com.morphengine.nexus.menu;

import com.morphengine.nexus.integration.curios.CuriosTerminals;
import com.morphengine.nexus.item.NexusTerminalItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the Nexus Terminal a player carries when they ask for it without holding it up: the main hand first, then the
 * off hand, the slots of Curios and the inventory.
 */
public final class TerminalSlots {

    private TerminalSlots() {
    }

    /**
     * @return the slot of the terminal to use: the first one that is bound to a network, else the first terminal there
     *         is, so that the player is told it is not bound; {@code null} when the player carries none
     */
    public static @Nullable TerminalSlot find(final Player player) {
        TerminalSlot unbound = null;
        for (final TerminalSlot slot : carrying(player)) {
            if (NexusTerminalItem.boundNexus(slot.stackOf(player)) != null) {
                return slot;
            }
            if (unbound == null) {
                unbound = slot;
            }
        }
        return unbound;
    }

    private static List<TerminalSlot> carrying(final Player player) {
        final List<TerminalSlot> slots = new ArrayList<>();
        for (final InteractionHand hand : InteractionHand.values()) {
            addIfTerminal(slots, player, new TerminalSlot.Hand(hand));
        }
        slots.addAll(CuriosTerminals.terminalsOf(player));
        for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
            addIfTerminal(slots, player, new TerminalSlot.Carried(index));
        }
        return slots;
    }

    private static void addIfTerminal(final List<TerminalSlot> slots, final Player player, final TerminalSlot slot) {
        if (slot.stackOf(player).getItem() instanceof NexusTerminalItem) {
            slots.add(slot);
        }
    }
}
