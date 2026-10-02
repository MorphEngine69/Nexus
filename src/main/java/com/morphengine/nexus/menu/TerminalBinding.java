package com.morphengine.nexus.menu;

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

    boolean stillValid(Player player);

    /**
     * Lets the host know its menu was closed, when the menu is removed.
     */
    void release();

    Component defaultTitle();
}
