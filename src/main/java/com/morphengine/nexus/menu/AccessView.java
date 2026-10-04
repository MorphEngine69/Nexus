package com.morphengine.nexus.menu;

import com.morphengine.nexus.access.Operators;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.security.Editor;
import com.morphengine.nexus.security.Member;
import com.morphengine.nexus.security.NetworkSecurity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * What the access tab of a Nexus panel shows: who may do what with the
 * network, whom its viewer could add, and the viewer's standing, from which
 * the panel works out what the viewer may change by asking the same rules
 * the server applies.
 *
 * @param candidates online players who are not members yet, nearest to the
 *                   Nexus first, at most {@value #MAX_CANDIDATES}, sorted by name
 * @param standing   the viewer's standing on the server
 */
public record AccessView(Role defaultRole, List<Member> members, List<NameAndId> candidates,
                         Editor.Standing standing) {

    /** Players offered to add at most, so the list stays short on a full server. */
    public static final int MAX_CANDIDATES = 64;

    public AccessView {
        Objects.requireNonNull(defaultRole, "defaultRole must not be null");
        members = List.copyOf(members);
        candidates = List.copyOf(candidates);
        Objects.requireNonNull(standing, "standing must not be null");
    }

    /**
     * @return the view {@code viewer} gets of {@code security}, the access of the
     *         network of the Nexus at {@code nexus}. Server side.
     */
    public static AccessView of(final NetworkSecurity security, final ServerPlayer viewer, final BlockPos nexus) {
        final List<ServerPlayer> online = new ArrayList<>();
        for (ServerPlayer player : viewer.level().getServer().getPlayerList().getPlayers()) {
            if (!(player instanceof FakePlayer) && security.member(player.getUUID()).isEmpty()) {
                online.add(player);
            }
        }
        final Vec3 centre = Vec3.atCenterOf(nexus);
        online.sort(Comparator.comparingDouble(player -> distanceFrom(player, viewer, centre)));
        final List<NameAndId> candidates = new ArrayList<>();
        for (ServerPlayer player : online.subList(0, Math.min(online.size(), MAX_CANDIDATES))) {
            candidates.add(player.nameAndId());
        }
        candidates.sort(Comparator.comparing(candidate -> candidate.name().toLowerCase(Locale.ROOT)));
        return new AccessView(security.defaultRole(), security.members(), candidates,
                Operators.isOperator(viewer) ? Editor.Standing.OPERATOR : Editor.Standing.PLAYER);
    }

    /**
     * @return a copy of the rules as the server has them, for asking what an
     *         edit would come to; changes to it go nowhere
     */
    public NetworkSecurity rules() {
        return NetworkSecurity.restore(defaultRole, members, () -> { });
    }

    /**
     * @return {@code viewer} as an editor, with the standing the server gave them
     */
    public Editor editor(final UUID viewer) {
        return new Editor(viewer, standing);
    }

    /**
     * @return players farther from the Nexus, and those in other dimensions, last
     */
    private static double distanceFrom(final ServerPlayer player, final ServerPlayer viewer, final Vec3 centre) {
        return player.level() == viewer.level() ? player.position().distanceToSqr(centre) : Double.MAX_VALUE;
    }
}
