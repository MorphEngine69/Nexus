package com.morphengine.nexus.security;

import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.PermissionState;
import com.morphengine.nexus.api.network.security.Role;

import java.util.Objects;
import java.util.UUID;

/**
 * One change to the access of a network, as an editor asks for it. Whether
 * the editor may make it is up to {@link NetworkSecurity#apply}.
 */
public sealed interface SecurityEdit {

    /**
     * Makes a player a member, as a {@link Role#USER}.
     *
     * @param name the player's current name
     */
    record AddMember(UUID player, String name) implements SecurityEdit {

        public AddMember {
            Objects.requireNonNull(player, "player must not be null");
            Objects.requireNonNull(name, "name must not be null");
        }
    }

    /**
     * Takes a member off the list; from then on they have the role of everyone else.
     */
    record RemoveMember(UUID player) implements SecurityEdit {

        public RemoveMember {
            Objects.requireNonNull(player, "player must not be null");
        }
    }

    /**
     * Gives a member another role, anything but {@link Role#OWNER}, which only
     * {@link TransferOwnership} hands on.
     */
    record ChangeRole(UUID player, Role role) implements SecurityEdit {

        public ChangeRole {
            Objects.requireNonNull(player, "player must not be null");
            Objects.requireNonNull(role, "role must not be null");
        }
    }

    /**
     * Grants or denies one permission to a member apart from their role, or
     * with {@link PermissionState#INHERIT} leaves it to the role again.
     */
    record Adjust(UUID player, Permission permission, PermissionState state) implements SecurityEdit {

        public Adjust {
            Objects.requireNonNull(player, "player must not be null");
            Objects.requireNonNull(permission, "permission must not be null");
            Objects.requireNonNull(state, "state must not be null");
        }
    }

    /**
     * Gives another role to everyone who is not a member.
     */
    record ChangeDefaultRole(Role role) implements SecurityEdit {

        public ChangeDefaultRole {
            Objects.requireNonNull(role, "role must not be null");
        }
    }

    /**
     * Makes a member the owner; the owner so far stays on as an admin.
     */
    record TransferOwnership(UUID player) implements SecurityEdit {

        public TransferOwnership {
            Objects.requireNonNull(player, "player must not be null");
        }
    }

    /**
     * Makes the editor the owner of a network nobody owns yet.
     *
     * @param name the editor's current name
     */
    record Claim(String name) implements SecurityEdit {

        public Claim {
            Objects.requireNonNull(name, "name must not be null");
        }
    }
}
