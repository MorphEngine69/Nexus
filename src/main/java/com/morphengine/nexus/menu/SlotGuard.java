package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.EnumSet;
import java.util.Set;

/**
 * Whether a click in a {@link GuardedMenu} may go ahead on the server: a click
 * on a slot takes what the slot takes, shift-clicking out of the inventory
 * what the menu says, and collecting a stack from every slot by double click
 * what all its slots together take. A click turned away does nothing; the
 * server then puts the client's view of the slots right.
 */
final class SlotGuard {

    private SlotGuard() {
    }

    static boolean allows(
            final AbstractContainerMenu menu, final int slotIndex, final ClickType input, final Player player) {
        if (!(player instanceof ServerPlayer) || !(menu instanceof GuardedMenu guarded)) {
            return true;
        }
        if (input == ClickType.PICKUP_ALL) {
            return holdsAll(guarded, player, everySlot(menu, guarded));
        }
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return true;
        }
        final Slot slot = menu.slots.get(slotIndex);
        final boolean fromInventory = slot.container instanceof Inventory;
        return holdsAll(guarded, player, input == ClickType.QUICK_MOVE && fromInventory
                ? guarded.quickMovePermissions() : guarded.permissionsFor(slot));
    }

    /**
     * @return what every slot of {@code menu} that is not the player's own takes, together
     */
    static Set<Permission> everySlot(final AbstractContainerMenu menu, final GuardedMenu guarded) {
        final Set<Permission> needed = EnumSet.noneOf(Permission.class);
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) {
                needed.addAll(guarded.permissionsFor(slot));
            }
        }
        return needed;
    }

    private static boolean holdsAll(final GuardedMenu guarded, final Player player, final Set<Permission> needed) {
        for (Permission permission : needed) {
            if (!guarded.permits(player, permission)) {
                return false;
            }
        }
        return true;
    }
}
