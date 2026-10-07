package com.morphengine.nexus.security;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.Role;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Who may do what with one network: its owner, its members with their roles
 * and adjustments, and the role of everyone else.
 *
 * <p>A network nobody has claimed, such as one built before access rules
 * existed, has no owner and gives everyone {@link #UNCLAIMED_DEFAULT}, so it
 * works as it always did until an operator claims it.
 *
 * <p>Who may change what: managing takes {@link Permission#MANAGE}, which the
 * owner and admins hold. An editor changes only members ranked below them,
 * never themselves, and gives only roles ranked below their own: admins manage
 * users, guests and blocked players, the owner manages admins too, and an
 * {@linkplain Editor.Standing#OPERATOR operator} stands above the owner. Only
 * the owner or an operator hands ownership on, and only an operator claims a
 * network nobody owns.
 *
 * <p>Changes come only through {@link #apply} and {@link #refreshName}. Each
 * one bumps the {@linkplain #revision revision} and runs the listener given
 * at creation, so the world saves it. Server thread only.
 */
public final class NetworkSecurity implements AccessPolicy {

    /** Members a network keeps at most, so its list stays small enough to send in one packet. */
    public static final int MAX_MEMBERS = 256;

    /** What a new network gives everyone who is not a member: nothing, until its owner says otherwise. */
    public static final Role NEW_NETWORK_DEFAULT = Role.BLOCKED;

    /** What a network nobody owns gives everyone: all a user may do, as before access rules existed. */
    public static final Role UNCLAIMED_DEFAULT = Role.USER;

    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final Runnable onChange;
    private @Nullable UUID owner;
    private Role defaultRole;
    private int revision;

    private NetworkSecurity(final Role defaultRole, final Runnable onChange) {
        this.defaultRole = defaultRole;
        this.onChange = Objects.requireNonNull(onChange, "onChange must not be null");
    }

    /**
     * @param onChange run after every change
     * @return the access of a new network, owned by the player who set it up
     */
    public static NetworkSecurity ownedBy(final UUID owner, final String name, final Runnable onChange) {
        final NetworkSecurity security = new NetworkSecurity(NEW_NETWORK_DEFAULT, onChange);
        security.members.put(owner, new Member(owner, name, Role.OWNER));
        security.owner = owner;
        return security;
    }

    /**
     * @param onChange run after every change
     * @return the access of a network nobody owns
     */
    public static NetworkSecurity unclaimed(final Runnable onChange) {
        return new NetworkSecurity(UNCLAIMED_DEFAULT, onChange);
    }

    /**
     * Restores what {@link #defaultRole} and {@link #members} returned, making
     * the best of a damaged save rather than losing it: of several owners the
     * first one stays and the others become admins, a member listed twice
     * counts once, members past {@value #MAX_MEMBERS} are dropped, and a
     * default role no network gives everyone becomes {@link Role#BLOCKED}.
     *
     * @param onChange run after every change from now on
     */
    public static NetworkSecurity restore(final Role defaultRole, final List<Member> saved, final Runnable onChange) {
        final NetworkSecurity security = new NetworkSecurity(
                defaultRole.isDefaultable() ? defaultRole : Role.BLOCKED, onChange);
        for (Member member : saved) {
            if (security.members.size() >= MAX_MEMBERS || security.members.containsKey(member.id())) {
                continue;
            }
            final boolean secondOwner = member.role() == Role.OWNER && security.owner != null;
            final Member kept = secondOwner ? member.withRole(Role.ADMIN) : member;
            if (kept.role() == Role.OWNER) {
                security.owner = kept.id();
            }
            security.members.put(kept.id(), kept);
        }
        return security;
    }

    @Override
    public boolean isAllowed(final UUID player, final Permission permission) {
        final Member member = members.get(player);
        return member != null ? member.grants(permission) : defaultRole.grants(permission);
    }

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    /**
     * @return the member's role, or for anyone else the {@linkplain #defaultRole default role}
     */
    public Role roleOf(final UUID player) {
        final Member member = members.get(player);
        return member != null ? member.role() : defaultRole;
    }

    public Optional<Member> member(final UUID player) {
        return Optional.ofNullable(members.get(player));
    }

    /**
     * @return every member, the owner included, in the order they joined; a snapshot
     */
    public List<Member> members() {
        return List.copyOf(members.values());
    }

    /**
     * @return the role of everyone who is not a member
     */
    public Role defaultRole() {
        return defaultRole;
    }

    /**
     * @return a number that changes with every change
     */
    public int revision() {
        return revision;
    }

    /**
     * Makes {@code edit} if {@code editor} may make it and it makes sense.
     */
    public EditResult apply(final Editor editor, final SecurityEdit edit) {
        return apply(editor, edit, Action.EXECUTE);
    }

    /**
     * Makes {@code edit} if {@code editor} may make it and it makes sense, or
     * with {@link Action#SIMULATE} only tells what would become of it, as a
     * panel does to offer only what its player may do.
     */
    public EditResult apply(final Editor editor, final SecurityEdit edit, final Action action) {
        Objects.requireNonNull(editor, "editor must not be null");
        Objects.requireNonNull(action, "action must not be null");
        if (!action.isExecute()) {
            return restore(defaultRole, members(), () -> { }).apply(editor, edit, Action.EXECUTE);
        }
        return switch (Objects.requireNonNull(edit, "edit must not be null")) {
            case SecurityEdit.AddMember add -> add(editor, add);
            case SecurityEdit.RemoveMember remove -> remove(editor, remove);
            case SecurityEdit.ChangeRole change -> changeRole(editor, change);
            case SecurityEdit.Adjust adjust -> adjust(editor, adjust);
            case SecurityEdit.ChangeDefaultRole change -> changeDefaultRole(editor, change.role());
            case SecurityEdit.TransferOwnership transfer -> transfer(editor, transfer.player());
            case SecurityEdit.Claim claim -> claim(editor, claim.name());
        };
    }

    /**
     * Takes the name a member goes by now, as seen when they are online.
     *
     * @return whether the member had another name until now
     */
    public boolean refreshName(final UUID player, final String name) {
        final Member member = members.get(player);
        final String fitted = Member.fitName(name, player);
        if (member == null || member.name().equals(fitted)) {
            return false;
        }
        members.put(player, member.withName(fitted));
        changed();
        return true;
    }

    private EditResult add(final Editor editor, final SecurityEdit.AddMember add) {
        if (!mayManage(editor)) {
            return EditResult.DENIED;
        }
        if (members.containsKey(add.player())) {
            return EditResult.UNCHANGED;
        }
        if (members.size() >= MAX_MEMBERS) {
            return EditResult.FULL;
        }
        return put(new Member(add.player(), Member.fitName(add.name(), add.player()), Role.USER));
    }

    private EditResult remove(final Editor editor, final SecurityEdit.RemoveMember remove) {
        final Member target = members.get(remove.player());
        final EditResult refusal = refusalToEdit(editor, target);
        if (refusal != null) {
            return refusal;
        }
        members.remove(remove.player());
        changed();
        return EditResult.APPLIED;
    }

    private EditResult changeRole(final Editor editor, final SecurityEdit.ChangeRole change) {
        final Member target = members.get(change.player());
        final EditResult refusal = change.role() == Role.OWNER ? EditResult.INVALID : refusalToEdit(editor, target);
        if (refusal != null || target == null) {
            return refusal != null ? refusal : EditResult.INVALID;
        }
        if (!rankOf(editor).isAbove(Rank.of(change.role()))) {
            return EditResult.DENIED;
        }
        return target.role() == change.role() ? EditResult.UNCHANGED : put(target.withRole(change.role()));
    }

    private EditResult adjust(final Editor editor, final SecurityEdit.Adjust adjust) {
        final Member target = members.get(adjust.player());
        final EditResult refusal = refusalToEdit(editor, target);
        if (refusal != null || target == null) {
            return refusal != null ? refusal : EditResult.INVALID;
        }
        if (!adjust.permission().isAdjustable() || !target.role().isAdjustable()) {
            return EditResult.INVALID;
        }
        return target.stateOf(adjust.permission()) == adjust.state() ? EditResult.UNCHANGED
                : put(target.withState(adjust.permission(), adjust.state()));
    }

    private EditResult changeDefaultRole(final Editor editor, final Role role) {
        if (!mayManage(editor)) {
            return EditResult.DENIED;
        }
        if (!role.isDefaultable()) {
            return EditResult.INVALID;
        }
        if (role == defaultRole) {
            return EditResult.UNCHANGED;
        }
        defaultRole = role;
        changed();
        return EditResult.APPLIED;
    }

    private EditResult transfer(final Editor editor, final UUID player) {
        final UUID current = owner;
        final Member target = members.get(player);
        if (current == null || target == null || player.equals(current)) {
            return EditResult.INVALID;
        }
        if (!editor.id().equals(current) && editor.standing() != Editor.Standing.OPERATOR) {
            return EditResult.DENIED;
        }
        members.computeIfPresent(current, (id, previous) -> previous.withRole(Role.ADMIN));
        members.put(player, target.withRole(Role.OWNER));
        owner = player;
        changed();
        return EditResult.APPLIED;
    }

    private EditResult claim(final Editor editor, final String name) {
        if (owner != null) {
            return EditResult.INVALID;
        }
        if (editor.standing() != Editor.Standing.OPERATOR) {
            return EditResult.DENIED;
        }
        owner = editor.id();
        return put(new Member(editor.id(), Member.fitName(name, editor.id()), Role.OWNER));
    }

    /**
     * @return why {@code editor} may not change {@code target}; {@code null} when
     *         they may. The owner is never changed this way, only handed on.
     */
    private @Nullable EditResult refusalToEdit(final Editor editor, final @Nullable Member target) {
        if (target == null || target.role() == Role.OWNER) {
            return EditResult.INVALID;
        }
        final boolean allowed = mayManage(editor) && !target.id().equals(editor.id())
                && rankOf(editor).isAbove(Rank.of(target.role()));
        return allowed ? null : EditResult.DENIED;
    }

    private boolean mayManage(final Editor editor) {
        return editor.standing() == Editor.Standing.OPERATOR || isAllowed(editor.id(), Permission.MANAGE);
    }

    private Rank rankOf(final Editor editor) {
        return editor.standing() == Editor.Standing.OPERATOR ? Rank.OPERATOR : Rank.of(roleOf(editor.id()));
    }

    private EditResult put(final Member member) {
        members.put(member.id(), member);
        changed();
        return EditResult.APPLIED;
    }

    private void changed() {
        revision++;
        onChange.run();
    }

    /**
     * How high an editor or a member stands when one changes the other, lowest first.
     */
    private enum Rank {
        MEMBER, ADMIN, OWNER, OPERATOR;

        static Rank of(final Role role) {
            return switch (role) {
                case OWNER -> OWNER;
                case ADMIN -> ADMIN;
                case USER, GUEST, BLOCKED -> MEMBER;
            };
        }

        boolean isAbove(final Rank other) {
            return compareTo(other) > 0;
        }
    }
}
