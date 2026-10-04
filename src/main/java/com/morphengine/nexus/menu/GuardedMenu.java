package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.Set;

/**
 * A menu of something under the access rules of a network. On the server,
 * what a click, a button or a payload would do in it is checked against the
 * rules first; the client is told what its player holds, to show it.
 */
public interface GuardedMenu {

    /**
     * @return on the server, whether {@code player} may do what takes
     *         {@code permission} with what the menu shows; on the client always
     *         yes, as the client never decides
     */
    boolean permits(Player player, Permission permission);

    /**
     * @return whether {@code player} may do what takes {@code permission} in
     *         {@code menu}; yes for a menu outside every network
     */
    static boolean permits(final AbstractContainerMenu menu, final Player player, final Permission permission) {
        return !(menu instanceof GuardedMenu guarded) || guarded.permits(player, permission);
    }

    /**
     * @return what a click on {@code slot} takes; nothing for a slot of the
     *         player's own inventory
     */
    Set<Permission> permissionsFor(Slot slot);

    /**
     * @return what shift-clicking a stack out of the player's inventory takes
     */
    Set<Permission> quickMovePermissions();

    /**
     * @return what the player looking at the menu holds where it is, as the
     *         server last worked it out
     */
    AccessSync viewerAccess();
}
