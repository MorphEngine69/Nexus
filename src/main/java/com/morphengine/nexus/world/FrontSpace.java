package com.morphengine.nexus.world;

import com.mojang.authlib.GameProfile;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.transfer.WorldMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * The block space the face of a Placer or Remover touches, and the device's
 * hand in it: a fake player standing at the device and looking through its
 * face, so that placing and breaking go through the game's usual rules and
 * the events protection mods listen to. The fake player goes by the id and
 * name of the player the device works for, so those rules treat what it does
 * as theirs; a device nobody owns works as a player of the mod's own.
 * Server thread only.
 */
public final class FrontSpace {

    private static final UUID NOBODY_ID = UUID.fromString("5f1e5c1a-6e2b-4c6d-9b0e-6e65787573ff");
    private static final GameProfile NOBODY = new GameProfile(NOBODY_ID, "[Nexus]");

    private final ServerLevel level;
    private final BlockPos device;
    private final Direction face;
    private final DeviceHand hand;

    /**
     * @param device where the device stands
     * @param face   the device's face; the space is the block beyond it
     */
    public FrontSpace(final ServerLevel level, final BlockPos device, final Direction face, final DeviceHand hand) {
        this.level = Objects.requireNonNull(level, "level must not be null");
        this.device = device.immutable();
        this.face = Objects.requireNonNull(face, "face must not be null");
        this.hand = Objects.requireNonNull(hand, "hand must not be null");
    }

    /**
     * @return whether {@code player} is the hand of a device nobody owns, whose
     *         work counts as the network's own
     */
    public static boolean standsInForNobody(final Player player) {
        return player instanceof FakePlayer && NOBODY_ID.equals(player.getUUID());
    }

    /**
     * @return the space as a place to put what the network gives, as blocks
     *         or as items dropped there
     */
    public Storage placement(final WorldMode mode) {
        return new BlockPlacement(this, mode);
    }

    /**
     * @return the items lying loose in the space
     */
    public Storage groundItems() {
        return new GroundItems(level, pos());
    }

    /**
     * @return the fluid source standing in the space, if any
     */
    public Storage fluidSource() {
        return new FluidSource(this);
    }

    /**
     * Breaks the block in the space when {@code filter} allows it as an item
     * and {@code network} takes everything it drops, and puts the drops there.
     *
     * @param actor who the drops go into the network for
     * @return units dropped into the network; zero when the block stays
     */
    public long harvest(final ResourceFilter filter, final Storage network, final Actor actor) {
        return new BlockHarvest(this, hand.tool()).harvest(filter, network, actor);
    }

    ServerLevel level() {
        return level;
    }

    BlockPos pos() {
        return device.relative(face);
    }

    BlockState state() {
        return level.getBlockState(pos());
    }

    /**
     * @return whether the space is loaded, so it can be looked into without loading it
     */
    boolean isLoaded() {
        return level.isLoaded(pos());
    }

    /**
     * @return the device's hand as a player, standing at the device, looking
     *         through its face, empty-handed
     */
    FakePlayer player() {
        final NameAndId owner = hand.owner();
        final FakePlayer player = FakePlayerFactory.get(level,
                owner != null ? new GameProfile(owner.id(), owner.name()) : NOBODY);
        final Vec3 centre = Vec3.atCenterOf(device);
        player.snapTo(centre.x, centre.y - player.getEyeHeight(), centre.z, face.toYRot(), pitchOf(face));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return player;
    }

    /**
     * @return a click on the space itself, from the device's side
     */
    BlockHitResult hit() {
        return new BlockHitResult(Vec3.atCenterOf(pos()), face.getOpposite(), pos(), false);
    }

    Vec3 centre() {
        return Vec3.atCenterOf(pos());
    }

    Direction face() {
        return face;
    }

    private static float pitchOf(final Direction face) {
        final float straightDown = 90;
        return switch (face) {
            case UP -> -straightDown;
            case DOWN -> straightDown;
            default -> 0;
        };
    }
}
