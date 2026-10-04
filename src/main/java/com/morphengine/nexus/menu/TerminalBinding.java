package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * What ties a terminal menu to what it was opened on, a terminal block or a
 * Nexus Terminal in a hand. A menu owns one and delegates to it.
 */
public interface TerminalBinding {

    /**
     * @return the host on the server; {@code null} on the client, or when the
     *         host was gone when the menu opened
     */
    @Nullable TerminalHost host();

    /**
     * @return the player the panel data is sent to; {@code null} on the client
     */
    @Nullable ServerPlayer viewer();

    /**
     * @return whether the menu stays open; on the server only while the player
     *         may still open what it is bound to
     */
    boolean stillValid(Player player);

    /**
     * @return on the server, whether {@code player} may do what takes
     *         {@code permission} in the network the menu works on; on the client always yes
     */
    boolean permits(Player player, Permission permission);

    /**
     * Lets the host know its menu was closed, when the menu is removed.
     */
    void release();

    Component defaultTitle();
}
