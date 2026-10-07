package com.morphengine.nexus.security;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.PermissionState;
import com.morphengine.nexus.api.network.security.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkSecurityTest {

    private static final UUID OWNER = id(1);
    private static final UUID ADMIN = id(2);
    private static final UUID USER = id(3);
    private static final UUID GUEST = id(4);
    private static final UUID STRANGER = id(5);
    private static final UUID OPERATOR = id(6);

    private final AtomicInteger changes = new AtomicInteger();
    private final NetworkSecurity security = NetworkSecurity.ownedBy(OWNER, "Owner", changes::incrementAndGet);

    NetworkSecurityTest() {
        asOwner(new SecurityEdit.AddMember(ADMIN, "Admin"));
        asOwner(new SecurityEdit.ChangeRole(ADMIN, Role.ADMIN));
        asOwner(new SecurityEdit.AddMember(USER, "User"));
        asOwner(new SecurityEdit.AddMember(GUEST, "Guest"));
        asOwner(new SecurityEdit.ChangeRole(GUEST, Role.GUEST));
        changes.set(0);
    }

    @Test
    void newNetworkBelongsToItsCreator() {
        assertThat(security.owner()).contains(OWNER);
        assertThat(security.roleOf(OWNER)).isEqualTo(Role.OWNER);
    }

    @Test
    void newNetworkShutsOutStrangers() {
        assertThat(security.defaultRole()).isEqualTo(Role.BLOCKED);
        assertThat(security.isAllowed(STRANGER, Permission.OPEN)).isFalse();
    }

    @Test
    void unclaimedNetworkLetsEveryoneWorkAsBefore() {
        final NetworkSecurity unclaimed = NetworkSecurity.unclaimed(() -> { });

        assertThat(unclaimed.owner()).isEmpty();
        assertThat(unclaimed.isAllowed(STRANGER, Permission.EXTRACT)).isTrue();
        assertThat(unclaimed.isAllowed(STRANGER, Permission.MANAGE)).isFalse();
    }

    @Test
    void membersHoldWhatTheirRoleGrants() {
        assertThat(security.isAllowed(USER, Permission.EXTRACT)).isTrue();
        assertThat(security.isAllowed(GUEST, Permission.EXTRACT)).isFalse();
        assertThat(security.isAllowed(GUEST, Permission.OPEN)).isTrue();
    }

    @Test
    void ownerHoldsEverything() {
        for (Permission permission : Permission.values()) {
            assertThat(security.isAllowed(OWNER, permission)).as(permission.name()).isTrue();
        }
    }

    @Test
    void addedPlayerBecomesAUser() {
        final EditResult result = asAdmin(new SecurityEdit.AddMember(STRANGER, "Stranger"));

        assertThat(result).isEqualTo(EditResult.APPLIED);
        assertThat(security.roleOf(STRANGER)).isEqualTo(Role.USER);
        assertThat(changes).hasValue(1);
    }

    @Test
    void addingAMemberAgainChangesNothing() {
        assertThat(asOwner(new SecurityEdit.AddMember(USER, "User"))).isEqualTo(EditResult.UNCHANGED);
        assertThat(changes).hasValue(0);
    }

    @Test
    void userCannotAddMembers() {
        final EditResult result = security.apply(Editor.player(USER), new SecurityEdit.AddMember(STRANGER, "S"));

        assertThat(result).isEqualTo(EditResult.DENIED);
        assertThat(security.member(STRANGER)).isEmpty();
    }

    @Test
    void networkStopsTakingMembersWhenFull() {
        for (int i = 0; security.members().size() < NetworkSecurity.MAX_MEMBERS; i++) {
            asOwner(new SecurityEdit.AddMember(id(100 + i), "Player" + i));
        }

        assertThat(asOwner(new SecurityEdit.AddMember(STRANGER, "Stranger"))).isEqualTo(EditResult.FULL);
    }

    @Test
    void adminRemovesAUser() {
        assertThat(asAdmin(new SecurityEdit.RemoveMember(USER))).isEqualTo(EditResult.APPLIED);
        assertThat(security.roleOf(USER)).isEqualTo(Role.BLOCKED);
    }

    @Test
    void adminCannotRemoveAnotherAdmin() {
        final UUID secondAdmin = id(7);
        asOwner(new SecurityEdit.AddMember(secondAdmin, "Second"));
        asOwner(new SecurityEdit.ChangeRole(secondAdmin, Role.ADMIN));

        assertThat(asAdmin(new SecurityEdit.RemoveMember(secondAdmin))).isEqualTo(EditResult.DENIED);
    }

    @Test
    void ownerRemovesAnAdmin() {
        assertThat(asOwner(new SecurityEdit.RemoveMember(ADMIN))).isEqualTo(EditResult.APPLIED);
    }

    @Test
    void nobodyRemovesTheOwner() {
        assertThat(asAdmin(new SecurityEdit.RemoveMember(OWNER))).isEqualTo(EditResult.INVALID);
        assertThat(asOperator(new SecurityEdit.RemoveMember(OWNER))).isEqualTo(EditResult.INVALID);
        assertThat(security.owner()).contains(OWNER);
    }

    @Test
    void editorCannotChangeThemselves() {
        assertThat(asAdmin(new SecurityEdit.ChangeRole(ADMIN, Role.USER))).isEqualTo(EditResult.DENIED);
        assertThat(asAdmin(new SecurityEdit.RemoveMember(ADMIN))).isEqualTo(EditResult.DENIED);
    }

    @Test
    void removingSomeoneWhoIsNotAMemberIsInvalid() {
        assertThat(asOwner(new SecurityEdit.RemoveMember(STRANGER))).isEqualTo(EditResult.INVALID);
    }

    @Test
    void adminGivesRolesBelowTheirOwn() {
        assertThat(asAdmin(new SecurityEdit.ChangeRole(USER, Role.GUEST))).isEqualTo(EditResult.APPLIED);
        assertThat(security.roleOf(USER)).isEqualTo(Role.GUEST);
    }

    @Test
    void adminCannotMakeAnotherAdmin() {
        assertThat(asAdmin(new SecurityEdit.ChangeRole(USER, Role.ADMIN))).isEqualTo(EditResult.DENIED);
        assertThat(security.roleOf(USER)).isEqualTo(Role.USER);
    }

    @Test
    void ownerMakesAnAdmin() {
        assertThat(asOwner(new SecurityEdit.ChangeRole(USER, Role.ADMIN))).isEqualTo(EditResult.APPLIED);
        assertThat(security.isAllowed(USER, Permission.MANAGE)).isTrue();
    }

    @Test
    void ownershipIsNotGivenAsARole() {
        assertThat(asOwner(new SecurityEdit.ChangeRole(USER, Role.OWNER))).isEqualTo(EditResult.INVALID);
        assertThat(security.owner()).contains(OWNER);
    }

    @Test
    void ownerRoleCannotBeChangedEvenByAnOperator() {
        assertThat(asOperator(new SecurityEdit.ChangeRole(OWNER, Role.USER))).isEqualTo(EditResult.INVALID);
        assertThat(security.roleOf(OWNER)).isEqualTo(Role.OWNER);
    }

    @Test
    void sameRoleChangesNothing() {
        assertThat(asOwner(new SecurityEdit.ChangeRole(USER, Role.USER))).isEqualTo(EditResult.UNCHANGED);
        assertThat(changes).hasValue(0);
    }

    @Test
    void blockedMemberHoldsNothing() {
        asAdmin(new SecurityEdit.ChangeRole(USER, Role.BLOCKED));

        assertThat(security.isAllowed(USER, Permission.OPEN)).isFalse();
    }

    @Test
    void adjustmentGrantsOnePermissionBeyondTheRole() {
        final EditResult result = asAdmin(new SecurityEdit.Adjust(GUEST, Permission.INSERT, PermissionState.ALLOW));

        assertThat(result).isEqualTo(EditResult.APPLIED);
        assertThat(security.isAllowed(GUEST, Permission.INSERT)).isTrue();
        assertThat(security.isAllowed(GUEST, Permission.EXTRACT)).isFalse();
    }

    @Test
    void adjustmentDeniesOnePermissionOfTheRole() {
        asAdmin(new SecurityEdit.Adjust(USER, Permission.EXTRACT, PermissionState.DENY));

        assertThat(security.isAllowed(USER, Permission.EXTRACT)).isFalse();
        assertThat(security.isAllowed(USER, Permission.INSERT)).isTrue();
    }

    @Test
    void inheritingLeavesThePermissionToTheRoleAgain() {
        asAdmin(new SecurityEdit.Adjust(USER, Permission.EXTRACT, PermissionState.DENY));

        asAdmin(new SecurityEdit.Adjust(USER, Permission.EXTRACT, PermissionState.INHERIT));

        assertThat(security.isAllowed(USER, Permission.EXTRACT)).isTrue();
    }

    @Test
    void managingIsNeverAdjusted() {
        final EditResult result = asOwner(new SecurityEdit.Adjust(USER, Permission.MANAGE, PermissionState.ALLOW));

        assertThat(result).isEqualTo(EditResult.INVALID);
        assertThat(security.isAllowed(USER, Permission.MANAGE)).isFalse();
    }

    @Test
    void blockedMemberTakesNoAdjustments() {
        asOwner(new SecurityEdit.ChangeRole(USER, Role.BLOCKED));

        assertThat(asOwner(new SecurityEdit.Adjust(USER, Permission.OPEN, PermissionState.ALLOW)))
                .isEqualTo(EditResult.INVALID);
    }

    @Test
    void adminCannotAdjustAnotherAdminButTheOwnerCan() {
        final UUID secondAdmin = id(7);
        asOwner(new SecurityEdit.AddMember(secondAdmin, "Second"));
        asOwner(new SecurityEdit.ChangeRole(secondAdmin, Role.ADMIN));
        final SecurityEdit denyBuild = new SecurityEdit.Adjust(secondAdmin, Permission.BUILD, PermissionState.DENY);

        assertThat(asAdmin(denyBuild)).isEqualTo(EditResult.DENIED);
        assertThat(asOwner(denyBuild)).isEqualTo(EditResult.APPLIED);
    }

    @Test
    void defaultRoleAppliesToEveryoneWhoIsNotAMember() {
        assertThat(asAdmin(new SecurityEdit.ChangeDefaultRole(Role.GUEST))).isEqualTo(EditResult.APPLIED);

        assertThat(security.isAllowed(STRANGER, Permission.OPEN)).isTrue();
        assertThat(security.isAllowed(STRANGER, Permission.EXTRACT)).isFalse();
    }

    @Test
    void adminIsNeverEveryonesDefault() {
        assertThat(asOwner(new SecurityEdit.ChangeDefaultRole(Role.ADMIN))).isEqualTo(EditResult.INVALID);
        assertThat(security.defaultRole()).isEqualTo(Role.BLOCKED);
    }

    @Test
    void userCannotChangeTheDefaultRole() {
        assertThat(security.apply(Editor.player(USER), new SecurityEdit.ChangeDefaultRole(Role.USER)))
                .isEqualTo(EditResult.DENIED);
    }

    @Test
    void strangerCannotManageAnything() {
        assertThat(security.apply(Editor.player(STRANGER), new SecurityEdit.AddMember(STRANGER, "Me")))
                .isEqualTo(EditResult.DENIED);
    }

    @Test
    void ownerHandsOwnershipOnAndStaysAsAdmin() {
        assertThat(asOwner(new SecurityEdit.TransferOwnership(USER))).isEqualTo(EditResult.APPLIED);

        assertThat(security.owner()).contains(USER);
        assertThat(security.roleOf(USER)).isEqualTo(Role.OWNER);
        assertThat(security.roleOf(OWNER)).isEqualTo(Role.ADMIN);
        assertThat(security.members()).filteredOn(member -> member.role() == Role.OWNER).hasSize(1);
    }

    @Test
    void adminCannotHandOwnershipOn() {
        assertThat(asAdmin(new SecurityEdit.TransferOwnership(ADMIN))).isEqualTo(EditResult.DENIED);
        assertThat(security.owner()).contains(OWNER);
    }

    @Test
    void ownershipGoesOnlyToAMember() {
        assertThat(asOwner(new SecurityEdit.TransferOwnership(STRANGER))).isEqualTo(EditResult.INVALID);
    }

    @Test
    void operatorHandsOwnershipOn() {
        assertThat(asOperator(new SecurityEdit.TransferOwnership(ADMIN))).isEqualTo(EditResult.APPLIED);
        assertThat(security.owner()).contains(ADMIN);
    }

    @Test
    void operatorManagesAdmins() {
        assertThat(asOperator(new SecurityEdit.ChangeRole(ADMIN, Role.GUEST))).isEqualTo(EditResult.APPLIED);
    }

    @Test
    void operatorClaimsANetworkNobodyOwns() {
        final NetworkSecurity unclaimed = NetworkSecurity.unclaimed(() -> { });

        final EditResult result = unclaimed.apply(Editor.operator(OPERATOR), new SecurityEdit.Claim("Op"));

        assertThat(result).isEqualTo(EditResult.APPLIED);
        assertThat(unclaimed.owner()).contains(OPERATOR);
        assertThat(unclaimed.defaultRole()).isEqualTo(NetworkSecurity.UNCLAIMED_DEFAULT);
    }

    @Test
    void playerCannotClaimANetworkNobodyOwns() {
        final NetworkSecurity unclaimed = NetworkSecurity.unclaimed(() -> { });

        final EditResult result = unclaimed.apply(Editor.player(STRANGER), new SecurityEdit.Claim("Me"));

        assertThat(result).isEqualTo(EditResult.DENIED);
        assertThat(unclaimed.owner()).isEmpty();
    }

    @Test
    void claimedNetworkCannotBeClaimedAgain() {
        assertThat(asOperator(new SecurityEdit.Claim("Op"))).isEqualTo(EditResult.INVALID);
        assertThat(security.owner()).contains(OWNER);
    }

    @Test
    void refreshedNameIsKept() {
        assertThat(security.refreshName(USER, "Renamed")).isTrue();

        assertThat(security.member(USER)).hasValueSatisfying(member -> assertThat(member.name()).isEqualTo("Renamed"));
        assertThat(changes).hasValue(1);
    }

    @Test
    void sameNameIsNoChange() {
        assertThat(security.refreshName(USER, "User")).isFalse();
        assertThat(security.refreshName(STRANGER, "Stranger")).isFalse();
        assertThat(changes).hasValue(0);
    }

    @Test
    void everyChangeBumpsTheRevision() {
        final int before = security.revision();

        asOwner(new SecurityEdit.ChangeDefaultRole(Role.GUEST));

        assertThat(security.revision()).isGreaterThan(before);
    }

    @Test
    void refusedChangeLeavesTheRevision() {
        final int before = security.revision();

        security.apply(Editor.player(GUEST), new SecurityEdit.RemoveMember(USER));

        assertThat(security.revision()).isEqualTo(before);
        assertThat(changes).hasValue(0);
    }

    @Test
    void restoreKeepsWhatWasSaved() {
        final NetworkSecurity restored = NetworkSecurity.restore(security.defaultRole(), security.members(), () -> { });

        assertThat(restored.owner()).isEqualTo(security.owner());
        assertThat(restored.members()).isEqualTo(security.members());
        assertThat(restored.defaultRole()).isEqualTo(security.defaultRole());
    }

    @Test
    void restoreKeepsOnlyTheFirstOfSeveralOwners() {
        final List<Member> damaged = List.of(new Member(OWNER, "First", Role.OWNER),
                new Member(USER, "Second", Role.OWNER, Map.of()));

        final NetworkSecurity restored = NetworkSecurity.restore(Role.BLOCKED, damaged, () -> { });

        assertThat(restored.owner()).contains(OWNER);
        assertThat(restored.roleOf(USER)).isEqualTo(Role.ADMIN);
    }

    @Test
    void restoreCountsAMemberListedTwiceOnce() {
        final List<Member> damaged = List.of(new Member(USER, "User", Role.USER), new Member(USER, "Dup", Role.GUEST));

        final NetworkSecurity restored = NetworkSecurity.restore(Role.BLOCKED, damaged, () -> { });

        assertThat(restored.members()).hasSize(1);
        assertThat(restored.roleOf(USER)).isEqualTo(Role.USER);
    }

    @Test
    void restoreTurnsADefaultNoNetworkGivesIntoBlocked() {
        final NetworkSecurity restored = NetworkSecurity.restore(Role.ADMIN, List.of(), () -> { });

        assertThat(restored.defaultRole()).isEqualTo(Role.BLOCKED);
    }

    @Test
    void restoreWithoutOwnerIsUnclaimed() {
        final NetworkSecurity restored = NetworkSecurity.restore(Role.USER, List.of(), () -> { });

        assertThat(restored.owner()).isEmpty();
    }

    @Test
    void simulatedEditTellsTheOutcomeWithoutMakingIt() {
        final EditResult result = security.apply(Editor.player(ADMIN), new SecurityEdit.RemoveMember(USER),
                Action.SIMULATE);

        assertThat(result).isEqualTo(EditResult.APPLIED);
        assertThat(security.member(USER)).isPresent();
        assertThat(changes).hasValue(0);
    }

    @Test
    void simulatedEditTellsARefusalToo() {
        assertThat(security.apply(Editor.player(GUEST), new SecurityEdit.RemoveMember(USER), Action.SIMULATE))
                .isEqualTo(EditResult.DENIED);
    }

    private EditResult asOwner(final SecurityEdit edit) {
        return security.apply(Editor.player(OWNER), edit);
    }

    private EditResult asAdmin(final SecurityEdit edit) {
        return security.apply(Editor.player(ADMIN), edit);
    }

    private EditResult asOperator(final SecurityEdit edit) {
        return security.apply(Editor.operator(OPERATOR), edit);
    }

    private static UUID id(final int number) {
        return new UUID(0, number);
    }
}
