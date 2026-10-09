package com.morphengine.nexus.access;

import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Asking whether a player may do something with a network, and telling them
 * when they may not.
 */
public final class NetworkAccess {

    private NetworkAccess() {
    }

    /**
     * @return whether {@code player} may do what takes {@code permission} where
     *         {@code policy} rules; an {@linkplain Operators operator} always may
     */
    public static boolean permits(final ServerPlayer player, final AccessPolicy policy, final Permission permission) {
        return policy.isAllowed(player.getUUID(), permission) || Operators.isOperator(player);
    }

    /**
     * Asked of a block entity on either side: the client never decides and
     * always hears yes, the server answers for real.
     *
     * @param secured what the player acts on, usually a block entity; {@code null} when there is nothing
     * @return whether {@code player} may do what takes {@code permission} with
     *         {@code secured}; yes for anything that is not {@link Secured}
     */
    public static boolean permits(final Player player, final @Nullable Object secured, final Permission permission) {
        return !(player instanceof ServerPlayer serverPlayer) || !(secured instanceof Secured block)
                || permits(serverPlayer, block.accessPolicy(), permission);
    }

    /**
     * Like {@link #permits(Player, Object, Permission)}, but whoever placed {@code secured} may always do it.
     */
    public static boolean permitsOrOwns(
            final Player player, final @Nullable Object secured, final Permission permission) {
        return secured instanceof Secured block && block.isOwnedBy(player.getUUID())
                || permits(player, secured, permission);
    }

    /**
     * Tells {@code player}, above the hotbar, that they lack {@code permission}.
     */
    public static void refuse(final Player player, final Permission permission) {
        player.displayClientMessage(Component.translatable("gui.nexus.access.denied", nameOf(permission))
                .withStyle(ChatFormatting.RED), true);
    }

    public static Component nameOf(final Permission permission) {
        return Component.translatable("gui.nexus.access.permission." + permission.name().toLowerCase(Locale.ROOT));
    }

    public static Component nameOf(final Role role) {
        return Component.translatable("gui.nexus.access.role." + role.name().toLowerCase(Locale.ROOT));
    }
}
