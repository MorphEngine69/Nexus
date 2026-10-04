package com.morphengine.nexus.access;

import com.morphengine.nexus.security.SecurityEdit;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jspecify.annotations.Nullable;

/**
 * Turns a change to a network's access that a client asked for into one the
 * server stands behind: player names come from the server, never from the
 * request, and only a player on the server, in any dimension, can be added.
 */
public final class AccessRequests {

    private AccessRequests() {
    }

    /**
     * @param editor who asked for the change
     * @return {@code requested} with the names the server knows; {@code null}
     *         for a player to add who is not on the server, or is a fake player of a mod
     */
    public static @Nullable SecurityEdit trusted(final ServerPlayer editor, final SecurityEdit requested) {
        return switch (requested) {
            case SecurityEdit.AddMember add -> {
                final ServerPlayer added = editor.level().getServer().getPlayerList().getPlayer(add.player());
                yield added != null && !(added instanceof FakePlayer)
                        ? new SecurityEdit.AddMember(added.getUUID(), added.getName().getString()) : null;
            }
            case SecurityEdit.Claim claim -> new SecurityEdit.Claim(editor.getName().getString());
            default -> requested;
        };
    }
}
