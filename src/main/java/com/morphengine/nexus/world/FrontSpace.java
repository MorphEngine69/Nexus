package com.morphengine.nexus.world;

import com.mojang.authlib.GameProfile;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.transfer.WorldMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
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
 * hand in it: a player of the mod's own standing at the device and looking
 * through its face, so that placing and breaking go through the game's usual
 * rules and the events protection mods listen to. Server thread only.
 */
public final class FrontSpace {

    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("5f1e5c1a-6e2b-4c6d-9b0e-6e65787573ff"), "[Nexus]");

    private final ServerLevel level;
    private final BlockPos device;
    private final Direction face;
    private final HarvestTool tool;

    /**
     * @param device where the device stands
     * @param face   the device's face; the space is the block beyond it
     */
    public FrontSpace(final ServerLevel level, final BlockPos device, final Direction face, final HarvestTool tool) {
        this.level = Objects.requireNonNull(level, "level must not be null");
        this.device = device.immutable();
        this.face = Objects.requireNonNull(face, "face must not be null");
        this.tool = Objects.requireNonNull(tool, "tool must not be null");
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
     * @return units dropped into the network; zero when the block stays
     */
    public long harvest(final ResourceFilter filter, final Storage network) {
        return new BlockHarvest(this, tool).harvest(filter, network);
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
     * @return the mod's player, standing at the device, looking through its face, empty-handed
     */
    FakePlayer player() {
        final FakePlayer player = FakePlayerFactory.get(level, PROFILE);
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
