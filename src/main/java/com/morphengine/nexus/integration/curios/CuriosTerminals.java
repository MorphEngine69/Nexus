package com.morphengine.nexus.integration.curios;

import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.menu.TerminalSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * Finds Nexus Terminals in the slots of Curios, when Curios is installed; without it there are none and nothing of
 * Curios is loaded.
 */
public final class CuriosTerminals {

    private static final String CURIOS_MOD = "curios";

    private CuriosTerminals() {
    }

    /**
     * @return what lies in the {@code index}th slot of {@code slotType} of {@code player}; empty when there is no such
     *         slot, or no Curios
     */
    public static ItemStack stackAt(final Player player, final String slotType, final int index) {
        return isInstalled() ? CuriosSlots.stackAt(player, slotType, index) : ItemStack.EMPTY;
    }

    /**
     * @return the slots of Curios of {@code player} that hold a Nexus Terminal, in the order Curios lists them
     */
    public static List<TerminalSlot.Curio> terminalsOf(final Player player) {
        return isInstalled() ? CuriosSlots.holding(player, stack -> stack.getItem() instanceof NexusTerminalItem)
                : List.of();
    }

    private static boolean isInstalled() {
        return ModList.get().isLoaded(CURIOS_MOD);
    }
}
