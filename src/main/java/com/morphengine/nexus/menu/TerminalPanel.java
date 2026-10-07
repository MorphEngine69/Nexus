package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.terminal.TerminalLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * A menu of a terminal of any kind, on a block or in a hand. Its title cannot
 * be renamed. What the terminal does with the network's storage is guarded by
 * the storage itself, by the player's rights; what the terminal keeps of its
 * own, such as a crafting grid, is guarded by its slots.
 */
public interface TerminalPanel extends PanelMenu, NetworkBadgeView, GuardedMenu {

    TerminalMenuState terminal();

    @Override
    default boolean permits(final Player player, final Permission permission) {
        return terminal().permits(player, permission);
    }

    /**
     * @return nothing: a terminal of its own has no slots but the player's
     */
    @Override
    default Set<Permission> permissionsFor(final Slot slot) {
        return Set.of();
    }

    /**
     * @return nothing: a stack shift-clicked out of the inventory goes into the
     *         network, which takes it only as far as the player may put in
     */
    @Override
    default Set<Permission> quickMovePermissions() {
        return Set.of();
    }

    @Override
    default AccessSync viewerAccess() {
        return terminal().access();
    }

    /**
     * Moves the slots to where {@code layout} puts them. The screen calls it
     * whenever it picks a layout; the server never needs slot positions.
     */
    void layOut(TerminalLayout layout);

    @Override
    default Component defaultTitle() {
        return terminal().binding().defaultTitle();
    }

    @Override
    default @Nullable NetworkBadge badge() {
        return terminal().badge();
    }

    @Override
    default void acceptBadge(final @Nullable NetworkBadge received) {
        terminal().acceptBadge(received);
    }
}
