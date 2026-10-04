package com.morphengine.nexus.access;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.PermissionState;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.security.Member;
import net.minecraft.core.UUIDUtil;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * How the access of networks is saved with the world. Names of roles,
 * permissions and states are written in lower case and read leniently, so a
 * save never fails to load over them: an unknown role reads as {@link
 * Role#BLOCKED}, which grants nothing, and an unknown permission or state is
 * left out.
 */
final class SecurityCodecs {

    static final Codec<Member> MEMBER = SavedMember.CODEC.xmap(SavedMember::toMember, SavedMember::of);

    static final Codec<Role> ROLE = Codec.STRING.xmap(SecurityCodecs::roleNamed, SecurityCodecs::nameOf);

    private SecurityCodecs() {
    }

    private static String nameOf(final Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static Role roleNamed(final String name) {
        final Role role = valueNamed(Role.class, name);
        return role != null ? role : Role.BLOCKED;
    }

    /**
     * @return the constant of {@code type} written as {@code name}; {@code null} when there is none
     */
    private static <E extends Enum<E>> @Nullable E valueNamed(final Class<E> type, final String name) {
        for (E value : type.getEnumConstants()) {
            if (nameOf(value).equals(name)) {
                return value;
            }
        }
        return null;
    }

    /**
     * A member as written in the save: names instead of enums, so it reads
     * whatever later versions add or drop.
     */
    private record SavedMember(UUID id, String name, String role, Map<String, String> adjustments) {

        static final Codec<SavedMember> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        UUIDUtil.CODEC.fieldOf("id").forGetter(SavedMember::id),
                        Codec.STRING.fieldOf("name").forGetter(SavedMember::name),
                        Codec.STRING.fieldOf("role").forGetter(SavedMember::role),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("adjustments", Map.of())
                                .forGetter(SavedMember::adjustments))
                .apply(instance, SavedMember::new));

        static SavedMember of(final Member member) {
            final Map<String, String> adjustments = new TreeMap<>();
            member.adjustments().forEach((permission, state) -> adjustments.put(nameOf(permission), nameOf(state)));
            return new SavedMember(member.id(), member.name(), nameOf(member.role()), adjustments);
        }

        Member toMember() {
            final Map<Permission, PermissionState> read = new EnumMap<>(Permission.class);
            adjustments.forEach((permissionName, stateName) -> {
                final Permission permission = valueNamed(Permission.class, permissionName);
                final PermissionState state = valueNamed(PermissionState.class, stateName);
                if (permission != null && state != null && permission.isAdjustable()) {
                    read.put(permission, state);
                }
            });
            return new Member(id, Member.fitName(name, id), roleNamed(role), read);
        }
    }
}
