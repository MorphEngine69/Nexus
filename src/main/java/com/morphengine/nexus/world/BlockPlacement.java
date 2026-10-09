package com.morphengine.nexus.world;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.StorageArguments;
import com.morphengine.nexus.transfer.WorldMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

/**
 * The block space in front of a Placer as a storage that only takes: an item
 * that is a block is placed there, as a player would, or with {@link
 * WorldMode#ITEMS} dropped there as items; a bucket's worth of a fluid is
 * poured there as a source. It holds nothing and gives nothing back. Server
 * thread only.
 */
final class BlockPlacement implements Storage {

    /** Millibuckets in a source block, what one placement of a fluid takes. */
    static final long BUCKET = 1000;
    private static final double THROW_SPEED = 0.1;

    private final FrontSpace space;
    private final WorldMode mode;

    BlockPlacement(final FrontSpace space, final WorldMode mode) {
        this.space = space;
        this.mode = mode;
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        if (!space.isLoaded()) {
            return 0;
        }
        return switch (resource) {
            case ItemKey item when mode == WorldMode.ITEMS -> drop(item, amount, action);
            case ItemKey item -> place(item, action);
            case FluidKey fluid when amount >= BUCKET -> pour(fluid.fluid().getFluid(), action) ? BUCKET : 0;
            default -> 0;
        };
    }

    /**
     * Places through the item's own use, as a player's click does, so that the
     * game fires the placement event protection rules listen to.
     */
    private long place(final ItemKey item, final Action action) {
        if (!(item.item().getItem() instanceof BlockItem) || !space.state().canBeReplaced()) {
            return 0;
        }
        if (!action.isExecute()) {
            return 1;
        }
        final FakePlayer player = space.player();
        final ItemStack stack = item.toStack(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final boolean placed = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, space.hit()))
                .consumesAction();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return placed ? 1 : 0;
    }

    private long drop(final ItemKey item, final long amount, final Action action) {
        final int count = (int) Math.min(amount, item.maxStackSize());
        if (action.isExecute()) {
            final Vec3 at = space.centre();
            final Vec3 motion = Vec3.atLowerCornerOf(space.face().getNormal()).scale(THROW_SPEED);
            space.level().addFreshEntity(new ItemEntity(space.level(), at.x, at.y, at.z, item.toStack(count),
                    motion.x, motion.y, motion.z));
        }
        return count;
    }

    /**
     * Pours a source of {@code fluid} into the space when it is empty or
     * replaceable and the fluid would not boil away there.
     */
    private boolean pour(final Fluid fluid, final Action action) {
        final ServerLevel level = space.level();
        final BlockPos pos = space.pos();
        final BlockState state = space.state();
        if (!(fluid instanceof FlowingFluid) || !state.canBeReplaced(fluid) || !state.getFluidState().isEmpty()
                || boilsAway(fluid)) {
            return false;
        }
        if (action.isExecute()) {
            if (!state.isAir()) {
                level.destroyBlock(pos, true);
            }
            level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL_IMMEDIATE);
            level.playSound(null, pos, fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA
                    : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return true;
    }

    private boolean boilsAway(final Fluid fluid) {
        return fluid.is(FluidTags.WATER)
                && space.level().dimensionType().ultraWarm();
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        return 0;
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return 0;
    }

    @Override
    public List<ResourceAmount> contents() {
        return List.of();
    }
}
