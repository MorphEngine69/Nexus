package com.morphengine.nexus.security;

import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.PermissionState;
import com.morphengine.nexus.api.network.security.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A member of a network: who they are, the name they had when last seen, their
 * role, and the permissions adjusted for them alone.
 *
 * @param name        shown to players only; the id is what counts
 * @param adjustments permissions held or not held apart from the role; only
 *                    {@link Permission#isAdjustable adjustable} ones, and never
 *                    {@link PermissionState#INHERIT}, which is what a missing
 *                    entry means. Kept while the role changes, but they count
 *                    only for a role that {@linkplain Role#isAdjustable takes} them.
 */
public record Member(UUID id, String name, Role role, Map<Permission, PermissionState> adjustments) {

    /** Longer than any player name a server hands out, to leave room for proxies that prefix them. */
    public static final int MAX_NAME_LENGTH = 32;

    private static final int FALLBACK_NAME_LENGTH = 8;

    /**
     * @throws IllegalArgumentException if the name is blank or longer than
     *                                  {@value #MAX_NAME_LENGTH}, or an
     *                                  adjustment is of {@link Permission#MANAGE}
     */
    public Member {
        Objects.requireNonNull(id, "id must not be null");
        name = checkedName(name);
        Objects.requireNonNull(role, "role must not be null");
        adjustments = adjustmentsOf(adjustments);
    }

    public Member(final UUID id, final String name, final Role role) {
        this(id, name, role, Map.of());
    }

    /**
     * @return {@code raw} as a member name: without surrounding blanks and cut
     *         to {@value #MAX_NAME_LENGTH} characters; the start of {@code id}
     *         when nothing is left, as for a save that lost the name
     */
    public static String fitName(final String raw, final UUID id) {
        final String stripped = raw.strip();
        if (stripped.isEmpty()) {
            return id.toString().substring(0, FALLBACK_NAME_LENGTH);
        }
        return stripped.length() > MAX_NAME_LENGTH ? stripped.substring(0, MAX_NAME_LENGTH) : stripped;
    }

    /**
     * Allocation-free, as it is asked on every operation.
     *
     * @return whether the member holds {@code permission}: the owner always, a
     *         blocked player never, anyone else by their role unless adjusted
     */
    public boolean grants(final Permission permission) {
        return switch (role) {
            case OWNER -> true;
            case BLOCKED -> false;
            default -> switch (stateOf(permission)) {
                case INHERIT -> role.grants(permission);
                case ALLOW -> true;
                case DENY -> false;
            };
        };
    }

    public PermissionState stateOf(final Permission permission) {
        return adjustments.getOrDefault(permission, PermissionState.INHERIT);
    }

    /**
     * @return this member with {@code newRole}; becoming the owner drops every
     *         adjustment, as the owner holds everything anyway
     */
    Member withRole(final Role newRole) {
        return new Member(id, name, newRole, newRole == Role.OWNER ? Map.of() : adjustments);
    }

    Member withName(final String newName) {
        return new Member(id, newName, role, adjustments);
    }

    Member withState(final Permission permission, final PermissionState state) {
        final Map<Permission, PermissionState> changed = new EnumMap<>(Permission.class);
        changed.putAll(adjustments);
        changed.put(permission, state);
        return new Member(id, name, role, changed);
    }

    private static String checkedName(final String candidate) {
        Objects.requireNonNull(candidate, "name must not be null");
        final String stripped = candidate.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException("member name must not be blank");
        }
        if (stripped.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "member name longer than " + MAX_NAME_LENGTH + " characters: " + stripped);
        }
        return stripped;
    }

    private static Map<Permission, PermissionState> adjustmentsOf(final Map<Permission, PermissionState> given) {
        Objects.requireNonNull(given, "adjustments must not be null");
        final Map<Permission, PermissionState> kept = new EnumMap<>(Permission.class);
        for (Map.Entry<Permission, PermissionState> entry : given.entrySet()) {
            if (!entry.getKey().isAdjustable()) {
                throw new IllegalArgumentException("permission is never adjusted: " + entry.getKey());
            }
            if (entry.getValue() != PermissionState.INHERIT) {
                kept.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(kept);
    }
}
