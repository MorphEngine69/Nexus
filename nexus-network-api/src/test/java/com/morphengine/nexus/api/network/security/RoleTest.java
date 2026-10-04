package com.morphengine.nexus.api.network.security;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

    @Test
    void ownerAndAdminHoldEveryPermission() {
        assertThat(Role.OWNER.permissions()).containsExactlyInAnyOrder(Permission.values());
        assertThat(Role.ADMIN.permissions()).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void userHoldsEverythingButManaging() {
        assertThat(Role.USER.permissions()).isEqualTo(EnumSet.complementOf(EnumSet.of(Permission.MANAGE)));
    }

    @Test
    void guestOnlyOpens() {
        assertThat(Role.GUEST.permissions()).containsExactly(Permission.OPEN);
    }

    @Test
    void blockedHoldsNothing() {
        assertThat(Role.BLOCKED.permissions()).isEmpty();
    }

    @Test
    void onlyRolesBelowAdminCanBeEveryonesDefault() {
        assertThat(EnumSet.allOf(Role.class)).filteredOn(Role::isDefaultable)
                .containsExactly(Role.USER, Role.GUEST, Role.BLOCKED);
    }

    @Test
    void ownerAndBlockedTakeNoAdjustments() {
        assertThat(EnumSet.allOf(Role.class)).filteredOn(Role::isAdjustable)
                .containsExactly(Role.ADMIN, Role.USER, Role.GUEST);
    }

    @Test
    void managingIsTheOnlyPermissionNeverAdjusted() {
        assertThat(EnumSet.allOf(Permission.class)).filteredOn(permission -> !permission.isAdjustable())
                .containsExactly(Permission.MANAGE);
    }

    @Test
    void permissionsOfARoleCannotBeChanged() {
        assertThatThrownBy(() -> Role.GUEST.permissions().add(Permission.EXTRACT))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
