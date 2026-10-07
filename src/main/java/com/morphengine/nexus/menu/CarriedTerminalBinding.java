package com.morphengine.nexus.menu;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.item.NexusTerminalItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * A terminal menu opened on a Nexus Terminal the player carries: it stays open
 * while that slot holds a Nexus Terminal, and when it closes the terminal gives
 * back what its grid and encoder held.
 */
final class CarriedTerminalBinding implements TerminalBinding {

    private final Player player;
    private final TerminalSlot slot;
    private final @Nullable PortableTerminal host;

    /**
     * @param host the terminal on the server; {@code null} on the client
     */
    CarriedTerminalBinding(final Player player, final TerminalSlot slot, final @Nullable PortableTerminal host) {
        this.player = player;
        this.slot = slot;
        this.host = host;
    }

    @Override
    public @Nullable TerminalHost host() {
        return host;
    }

    @Override
    public @Nullable ServerPlayer viewer() {
        return player instanceof ServerPlayer serverPlayer && host != null ? serverPlayer : null;
    }

    @Override
    public boolean stillValid(final Player clicker) {
        return slot.stackOf(clicker).getItem() instanceof NexusTerminalItem
                && permits(clicker, Permission.OPEN);
    }

    @Override
    public boolean permits(final Player clicker, final Permission permission) {
        return host == null || NetworkAccess.permits(clicker, host, permission);
    }

    @Override
    public void release() {
        if (host != null) {
            host.giveBack();
        }
    }

    @Override
    public Component defaultTitle() {
        return slot.stackOf(player).getHoverName();
    }
}
