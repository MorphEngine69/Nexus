package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.level.DeviceActor;
import com.morphengine.nexus.security.Member;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * The player a device works for, as the player who placed it, and the network
 * it was last in. Both stay with the block, not with its item: a device placed
 * again works for whoever placed it then.
 */
final class DeviceOwner {

    private static final String TAG_OWNER = "owner";
    private static final String TAG_OWNER_NAME = "owner_name";
    private static final String TAG_NETWORK = "last_network";

    private @Nullable UUID id;
    private String name = "";
    private @Nullable UUID lastNetwork;

    /**
     * @return whether the device has an owner from now on, as it had none before
     */
    boolean claimFor(final UUID player, final String playerName) {
        if (id != null) {
            return false;
        }
        id = Objects.requireNonNull(player, "player must not be null");
        name = Member.fitName(playerName, player);
        return true;
    }

    /**
     * Makes {@code player} the owner whoever owned the device before, and
     * forgets the network it was in, as for a device that has just been placed:
     * an item copied with its block's data carries both, and neither may pass on.
     */
    void placedBy(final UUID player, final String playerName) {
        id = null;
        lastNetwork = null;
        claimFor(player, playerName);
    }

    boolean isOwnedBy(final UUID player) {
        return player.equals(id);
    }

    boolean hasOwner() {
        return id != null;
    }

    /**
     * Allocation-free.
     *
     * @return whether {@code player} is the owner, or the device has none
     */
    boolean isOwnerOrNobody(final UUID player) {
        return id == null || id.equals(player);
    }

    DeviceActor actorNamed(final String deviceName) {
        return new DeviceActor(id, deviceName);
    }

    /**
     * @return the owner's id and name as last seen; {@code null} when the device has none
     */
    @Nullable NameAndId profile() {
        return id != null ? new NameAndId(id, name) : null;
    }

    /**
     * @return the network the device was last in; {@code null} before it was in any
     */
    @Nullable UUID lastNetwork() {
        return lastNetwork;
    }

    /**
     * @return whether the network differs from the one remembered so far
     */
    boolean remember(final UUID network) {
        if (network.equals(lastNetwork)) {
            return false;
        }
        lastNetwork = network;
        return true;
    }

    void save(final ValueOutput output) {
        output.storeNullable(TAG_OWNER, UUIDUtil.CODEC, id);
        if (id != null) {
            output.putString(TAG_OWNER_NAME, name);
        }
        output.storeNullable(TAG_NETWORK, UUIDUtil.CODEC, lastNetwork);
    }

    void load(final ValueInput input) {
        id = input.read(TAG_OWNER, UUIDUtil.CODEC).orElse(null);
        name = id != null ? Member.fitName(input.getStringOr(TAG_OWNER_NAME, ""), id) : "";
        lastNetwork = input.read(TAG_NETWORK, UUIDUtil.CODEC).orElse(null);
    }
}
