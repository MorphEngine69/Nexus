package com.morphengine.nexus.networking;

import com.morphengine.nexus.access.NameAndId;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.PermissionState;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.menu.AccessView;
import com.morphengine.nexus.security.Editor;
import com.morphengine.nexus.security.Member;
import com.morphengine.nexus.security.NetworkSecurity;
import com.morphengine.nexus.security.SecurityEdit;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wire format of the access of a network, as the Nexus panel sends and
 * receives it. A name longer than {@value Member#MAX_NAME_LENGTH} characters,
 * or an edit of an unknown kind, fails to decode.
 */
final class AccessCodecs {

    static final StreamCodec<RegistryFriendlyByteBuf, Role> ROLE = NeoForgeStreamCodecs.enumCodec(Role.class);

    static final StreamCodec<RegistryFriendlyByteBuf, Permission> PERMISSION =
            NeoForgeStreamCodecs.enumCodec(Permission.class);

    static final StreamCodec<RegistryFriendlyByteBuf, PermissionState> STATE =
            NeoForgeStreamCodecs.enumCodec(PermissionState.class);

    static final StreamCodec<ByteBuf, String> NAME = ByteBufCodecs.stringUtf8(Member.MAX_NAME_LENGTH);

    static final StreamCodec<RegistryFriendlyByteBuf, Map<Permission, PermissionState>> ADJUSTMENTS =
            ByteBufCodecs.map(size -> new EnumMap<>(Permission.class), PERMISSION, STATE,
                    Permission.values().length);

    static final StreamCodec<RegistryFriendlyByteBuf, Member> MEMBER = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Member::id,
            NAME, Member::name,
            ROLE, Member::role,
            ADJUSTMENTS, Member::adjustments,
            Member::new);

    static final StreamCodec<RegistryFriendlyByteBuf, NameAndId> PLAYER = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, NameAndId::id,
            NAME, NameAndId::name,
            NameAndId::new);

    static final StreamCodec<RegistryFriendlyByteBuf, AccessView> VIEW = StreamCodec.composite(
            ROLE, AccessView::defaultRole,
            MEMBER.apply(ByteBufCodecs.list(NetworkSecurity.MAX_MEMBERS)), AccessView::members,
            PLAYER.apply(ByteBufCodecs.list(AccessView.MAX_CANDIDATES)), AccessView::candidates,
            NeoForgeStreamCodecs.enumCodec(Editor.Standing.class), AccessView::standing,
            AccessView::new);

    static final StreamCodec<RegistryFriendlyByteBuf, SecurityEdit> EDIT =
            StreamCodec.of(AccessCodecs::writeEdit, AccessCodecs::readEdit);

    static final StreamCodec<RegistryFriendlyByteBuf, EditKind> KIND =
            NeoForgeStreamCodecs.enumCodec(EditKind.class);

    private AccessCodecs() {
    }

    private static void writeEdit(final RegistryFriendlyByteBuf buffer, final SecurityEdit edit) {
        switch (edit) {
            case SecurityEdit.AddMember add -> {
                KIND.encode(buffer, EditKind.ADD_MEMBER);
                writePlayer(buffer, add.player());
                NAME.encode(buffer, add.name());
            }
            case SecurityEdit.RemoveMember remove -> {
                KIND.encode(buffer, EditKind.REMOVE_MEMBER);
                writePlayer(buffer, remove.player());
            }
            case SecurityEdit.ChangeRole change -> {
                KIND.encode(buffer, EditKind.CHANGE_ROLE);
                writePlayer(buffer, change.player());
                ROLE.encode(buffer, change.role());
            }
            case SecurityEdit.Adjust adjust -> {
                KIND.encode(buffer, EditKind.ADJUST);
                writePlayer(buffer, adjust.player());
                PERMISSION.encode(buffer, adjust.permission());
                STATE.encode(buffer, adjust.state());
            }
            case SecurityEdit.ChangeDefaultRole change -> {
                KIND.encode(buffer, EditKind.CHANGE_DEFAULT_ROLE);
                ROLE.encode(buffer, change.role());
            }
            case SecurityEdit.TransferOwnership transfer -> {
                KIND.encode(buffer, EditKind.TRANSFER_OWNERSHIP);
                writePlayer(buffer, transfer.player());
            }
            case SecurityEdit.Claim claim -> {
                KIND.encode(buffer, EditKind.CLAIM);
                NAME.encode(buffer, claim.name());
            }
        }
    }

    private static SecurityEdit readEdit(final RegistryFriendlyByteBuf buffer) {
        return switch (KIND.decode(buffer)) {
            case ADD_MEMBER -> new SecurityEdit.AddMember(readPlayer(buffer), NAME.decode(buffer));
            case REMOVE_MEMBER -> new SecurityEdit.RemoveMember(readPlayer(buffer));
            case CHANGE_ROLE -> new SecurityEdit.ChangeRole(readPlayer(buffer), ROLE.decode(buffer));
            case ADJUST -> new SecurityEdit.Adjust(readPlayer(buffer), PERMISSION.decode(buffer),
                    STATE.decode(buffer));
            case CHANGE_DEFAULT_ROLE -> new SecurityEdit.ChangeDefaultRole(ROLE.decode(buffer));
            case TRANSFER_OWNERSHIP -> new SecurityEdit.TransferOwnership(readPlayer(buffer));
            case CLAIM -> new SecurityEdit.Claim(NAME.decode(buffer));
        };
    }

    private static void writePlayer(final RegistryFriendlyByteBuf buffer, final UUID player) {
        UUIDUtil.STREAM_CODEC.encode(buffer, player);
    }

    private static UUID readPlayer(final RegistryFriendlyByteBuf buffer) {
        return UUIDUtil.STREAM_CODEC.decode(buffer);
    }

    /**
     * Which {@link SecurityEdit} follows on the wire.
     */
    private enum EditKind {
        ADD_MEMBER, REMOVE_MEMBER, CHANGE_ROLE, ADJUST, CHANGE_DEFAULT_ROLE, TRANSFER_OWNERSHIP, CLAIM
    }
}
