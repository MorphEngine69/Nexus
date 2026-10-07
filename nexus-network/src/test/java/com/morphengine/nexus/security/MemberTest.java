package com.morphengine.nexus.security;

import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.PermissionState;
import com.morphengine.nexus.api.network.security.Role;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Test
    void memberHoldsWhatTheRoleGrants() {
        final Member guest = new Member(ID, "Alice", Role.GUEST);

        assertThat(guest.grants(Permission.OPEN)).isTrue();
        assertThat(guest.grants(Permission.EXTRACT)).isFalse();
    }

    @Test
    void allowedAdjustmentGrantsBeyondTheRole() {
        final Member guest = new Member(ID, "Alice", Role.GUEST, Map.of(Permission.INSERT, PermissionState.ALLOW));

        assertThat(guest.grants(Permission.INSERT)).isTrue();
    }

    @Test
    void deniedAdjustmentTakesAwayFromTheRole() {
        final Member user = new Member(ID, "Alice", Role.USER, Map.of(Permission.EXTRACT, PermissionState.DENY));

        assertThat(user.grants(Permission.EXTRACT)).isFalse();
        assertThat(user.grants(Permission.INSERT)).isTrue();
    }

    @Test
    void blockedMemberHoldsNothingWhateverTheAdjustments() {
        final Member blocked = new Member(ID, "Alice", Role.BLOCKED, Map.of(Permission.OPEN, PermissionState.ALLOW));

        assertThat(blocked.grants(Permission.OPEN)).isFalse();
    }

    @Test
    void ownerHoldsEverythingWhateverTheAdjustments() {
        final Member owner = new Member(ID, "Alice", Role.OWNER, Map.of(Permission.BUILD, PermissionState.DENY));

        assertThat(owner.grants(Permission.BUILD)).isTrue();
    }

    @Test
    void inheritedAdjustmentIsNotKept() {
        final Member user = new Member(ID, "Alice", Role.USER, Map.of(Permission.BUILD, PermissionState.INHERIT));

        assertThat(user.adjustments()).isEmpty();
        assertThat(user.stateOf(Permission.BUILD)).isEqualTo(PermissionState.INHERIT);
    }

    @Test
    void managingCannotBeAdjusted() {
        assertThatThrownBy(() -> new Member(ID, "Alice", Role.USER, Map.of(Permission.MANAGE, PermissionState.ALLOW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MANAGE");
    }

    @Test
    void becomingOwnerDropsAdjustments() {
        final Member admin = new Member(ID, "Alice", Role.ADMIN, Map.of(Permission.BUILD, PermissionState.DENY));

        assertThat(admin.withRole(Role.OWNER).adjustments()).isEmpty();
    }

    @Test
    void otherRoleKeepsAdjustments() {
        final Member user = new Member(ID, "Alice", Role.USER, Map.of(Permission.BUILD, PermissionState.DENY));

        assertThat(user.withRole(Role.GUEST).stateOf(Permission.BUILD)).isEqualTo(PermissionState.DENY);
    }

    @Test
    void blankNameIsRejected() {
        assertThatThrownBy(() -> new Member(ID, "  ", Role.USER)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tooLongNameIsRejected() {
        assertThatThrownBy(() -> new Member(ID, "a".repeat(Member.MAX_NAME_LENGTH + 1), Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nameIsStripped() {
        assertThat(new Member(ID, "  Alice ", Role.USER).name()).isEqualTo("Alice");
    }

    @Test
    void fittingCutsALongName() {
        assertThat(Member.fitName("b".repeat(40), ID)).hasSize(Member.MAX_NAME_LENGTH);
    }

    @Test
    void fittingABlankNameFallsBackToTheId() {
        assertThat(Member.fitName(" ", ID)).isEqualTo("00000000");
    }

    @Test
    void adjustmentsCannotBeChangedFromOutside() {
        final Member user = new Member(ID, "Alice", Role.USER);

        assertThatThrownBy(() -> user.adjustments().put(Permission.OPEN, PermissionState.DENY))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
