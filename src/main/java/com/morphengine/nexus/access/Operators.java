package com.morphengine.nexus.access;

import com.morphengine.nexus.security.Editor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Players who stand above every network: the operators of a server, and the
 * player whose world it is, playing alone or hosting it for the local network.
 * They may always do everything, as they could with commands anyway. A fake
 * player of a mod never does, whoever it stands in for.
 */
public final class Operators {

    private Operators() {
    }

    /**
     * Not for every tick: it looks the player up in the operator list.
     */
    public static boolean isOperator(final ServerPlayer player) {
        if (player instanceof FakePlayer) {
            return false;
        }
        final MinecraftServer server = player.level().getServer();
        return server.getPlayerList().isOp(player.getGameProfile())
                || server.isSingleplayerOwner(player.getGameProfile());
    }

    /**
     * @return {@code player} as the editor of a network's access, with the
     *         standing of an operator where they are one
     */
    public static Editor editorOf(final ServerPlayer player) {
        return isOperator(player) ? Editor.operator(player.getUUID()) : Editor.player(player.getUUID());
    }
}
