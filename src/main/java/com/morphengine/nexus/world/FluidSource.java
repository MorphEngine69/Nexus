package com.morphengine.nexus.world;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.storage.StorageArguments;
import com.morphengine.nexus.transfer.FluidResource;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The fluid source standing in the space in front of a Remover as a storage
 * that only gives: it holds a bucket's worth of the fluid while a source
 * stands there that a bucket could take, and gives it only whole, taking the
 * source away as a bucket would. Server thread only.
 */
final class FluidSource implements Storage {

    private final FrontSpace space;

    FluidSource(final FrontSpace space) {
        this.space = space;
    }

    /**
     * @return the fluid of the source standing there; {@code null} when none a bucket could take does
     */
    private @Nullable FluidKey source() {
        if (!space.isLoaded()) {
            return null;
        }
        final BlockState state = space.state();
        final FluidState fluid = state.getFluidState();
        if (!fluid.isSource() || !(state.getBlock() instanceof BucketPickup)) {
            return null;
        }
        return new FluidKey(FluidResource.of(fluid.getType()));
    }

    @Override
    public List<ResourceAmount> contents() {
        final FluidKey source = source();
        return source == null ? List.of() : List.of(new ResourceAmount(source, BlockPlacement.BUCKET));
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return resource.equals(source()) ? BlockPlacement.BUCKET : 0;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        if (amount < BlockPlacement.BUCKET || !resource.equals(source())) {
            return 0;
        }
        if (action.isExecute()) {
            final BlockState state = space.state();
            if (!(state.getBlock() instanceof BucketPickup pickup)
                    || pickup.pickupBlock(space.player(), space.level(), space.pos(), state).isEmpty()) {
                return 0;
            }
        }
        return BlockPlacement.BUCKET;
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        return 0;
    }
}
