package com.morphengine.nexus.integration.curios;

import com.morphengine.nexus.menu.TerminalSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.function.Predicate;

/**
 * The part of the integration that talks to Curios. Loaded only when Curios is installed.
 */
final class CuriosSlots {

    private CuriosSlots() {
    }

    static ItemStack stackAt(final Player player, final String slotType, final int index) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.findCurio(slotType, index))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    static List<TerminalSlot.Curio> holding(final Player player, final Predicate<ItemStack> test) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(test))
                .orElse(List.of())
                .stream()
                .map(SlotResult::slotContext)
                .map(CuriosSlots::slotOf)
                .toList();
    }

    private static TerminalSlot.Curio slotOf(final SlotContext context) {
        return new TerminalSlot.Curio(context.identifier(), context.index());
    }
}
