package com.morphengine.nexus.block.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A block entity whose device opens a menu on right click.
 */
public interface MenuHost extends MenuProvider {

    /**
     * @return what tells the held right button apart from a new click on the host
     */
    ClickGuard clickGuard();

    /**
     * @return the level the host is in; {@code null} while it is in none
     */
    @Nullable Level getLevel();

    /**
     * Called by the menu when the player closes it.
     */
    default void markClosed() {
        clickGuard().markClosed(getLevel());
    }

    /**
     * Called by the block when a player places it.
     */
    default void markPlaced() {
        clickGuard().markPlaced(getLevel());
    }

    /**
     * @return whether a click now is the held right button repeating after the
     *         device was placed or its menu closed, not a new request to open it
     */
    default boolean ignoresClick() {
        return clickGuard().ignoresClick(getLevel());
    }

    /**
     * Writes what the client menu needs besides the device's position, which is
     * always written first. Server side only.
     */
    default void writeMenuData(final RegistryFriendlyByteBuf buffer) {
    }
}
