package com.morphengine.nexus.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * What the block a device's face touches offers on the side it is touched:
 * its items, fluids and energy. The lookups are cached until they are asked
 * for another position or face. Server thread only.
 */
public final class NeighbourCapabilities {

    private @Nullable BlockPos watchedPos;
    private @Nullable Direction watchedFace;
    private @Nullable Caches caches;

    /**
     * @param pos  where the device stands
     * @param face the device's face; the neighbour is seen from the opposite side
     */
    public SideStorage items(final ServerLevel level, final BlockPos pos, final Direction face) {
        return new SideStorage(watch(level, pos, face).items().getCapability(), null, null);
    }

    public SideStorage fluids(final ServerLevel level, final BlockPos pos, final Direction face) {
        return new SideStorage(null, watch(level, pos, face).fluids().getCapability(), null);
    }

    public SideStorage energy(final ServerLevel level, final BlockPos pos, final Direction face) {
        return new SideStorage(null, null, watch(level, pos, face).energy().getCapability());
    }

    /**
     * @return the neighbour's items and fluids together, as a machine takes its inputs
     */
    public SideStorage itemsAndFluids(final ServerLevel level, final BlockPos pos, final Direction face) {
        final Caches watched = watch(level, pos, face);
        return new SideStorage(watched.items().getCapability(), watched.fluids().getCapability(), null);
    }

    private Caches watch(final ServerLevel level, final BlockPos pos, final Direction face) {
        final Caches current = caches;
        if (current != null && face == watchedFace && pos.equals(watchedPos)) {
            return current;
        }
        final BlockPos target = pos.relative(face);
        final Direction side = face.getOpposite();
        final Caches created = new Caches(
                BlockCapabilityCache.create(Capabilities.Item.BLOCK, level, target, side),
                BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, target, side),
                BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, target, side));
        caches = created;
        watchedPos = pos.immutable();
        watchedFace = face;
        return created;
    }

    private record Caches(
            BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> items,
            BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> fluids,
            BlockCapabilityCache<EnergyHandler, Direction> energy) {
    }
}
