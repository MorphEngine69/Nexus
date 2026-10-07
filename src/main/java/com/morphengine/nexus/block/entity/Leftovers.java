package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * What a device held when it was broken: given back to its network, and what
 * the network does not take dropped where the device stood. Server thread only.
 */
final class Leftovers {

    private static final Logger LOGGER = LoggerFactory.getLogger(Leftovers.class);

    private Leftovers() {
    }

    /**
     * @param network the device's network; {@code null} when it had none, and everything drops
     */
    static void giveBack(final Level level, final BlockPos pos, final @Nullable Storage network,
                         final List<ResourceAmount> left) {
        for (ResourceAmount amount : left) {
            final long inserted = network != null
                    ? network.insert(amount.resource(), amount.amount(), Action.EXECUTE, Actor.NOBODY) : 0;
            final long rest = amount.amount() - inserted;
            if (rest > 0 && amount.resource() instanceof ItemKey item) {
                drop(level, pos, item, rest);
            } else if (rest > 0) {
                LOGGER.warn("Device at {} was broken holding {} of {} the network did not take",
                        pos, rest, NexusResources.of(amount.resource()).id());
            }
        }
    }

    /**
     * Drops {@code amount} of {@code item} at {@code pos}, in stacks of its size.
     */
    static void drop(final Level level, final BlockPos pos, final ItemKey item, final long amount) {
        long left = amount;
        while (left > 0) {
            final int count = (int) Math.min(left, item.maxStackSize());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item.toStack(count));
            left -= count;
        }
    }
}
