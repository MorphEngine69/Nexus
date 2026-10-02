package com.morphengine.nexus.world;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.ResourceCounter;
import com.morphengine.nexus.storage.StorageArguments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The items lying loose in one block space as a storage that only gives, for
 * a Remover picking them up: it holds what the item entities there hold and
 * takes from them, oldest first. Server thread only.
 */
final class GroundItems implements Storage {

    private final ServerLevel level;
    private final BlockPos pos;

    GroundItems(final ServerLevel level, final BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    private List<ItemEntity> entities() {
        if (!level.isLoaded(pos)) {
            return List.of();
        }
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos), Entity::isAlive);
    }

    @Override
    public List<ResourceAmount> contents() {
        final ResourceCounter counter = new ResourceCounter();
        for (ItemEntity entity : entities()) {
            final ItemStack stack = entity.getItem();
            if (!stack.isEmpty()) {
                counter.add(ItemKey.of(stack), stack.getCount());
            }
        }
        return counter.contents();
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        long amount = 0;
        for (ItemEntity entity : entities()) {
            final ItemStack stack = entity.getItem();
            if (!stack.isEmpty() && ItemKey.of(stack).equals(resource)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        long taken = 0;
        for (ItemEntity entity : entities()) {
            final ItemStack stack = entity.getItem();
            if (taken >= amount || stack.isEmpty() || !ItemKey.of(stack).equals(resource)) {
                continue;
            }
            final int count = (int) Math.min(amount - taken, stack.getCount());
            if (action.isExecute()) {
                takeFrom(entity, stack, count);
            }
            taken += count;
        }
        return taken;
    }

    private static void takeFrom(final ItemEntity entity, final ItemStack stack, final int count) {
        if (count >= stack.getCount()) {
            entity.discard();
        } else {
            entity.setItem(stack.copyWithCount(stack.getCount() - count));
        }
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        return 0;
    }
}
