package com.morphengine.nexus.processing;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.machine.MachineSlot;
import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link MachineSlot} that is one slot of a container: the stack in it is the stack of the slot, so a menu that
 * changes the stack in place changes the slot, and the other way round. Holds items only.
 */
final class ItemStackMachineSlot implements MachineSlot {

    private final Container container;
    private final int index;

    ItemStackMachineSlot(final Container container, final int index) {
        this.container = Objects.requireNonNull(container, "container must not be null");
        this.index = index;
    }

    @Override
    public @Nullable ResourceKey resource() {
        final ItemStack stack = container.getItem(index);
        return stack.isEmpty() ? null : ItemKey.of(stack);
    }

    @Override
    public long amount() {
        return container.getItem(index).getCount();
    }

    @Override
    public long room(final ResourceKey wanted) {
        if (!(wanted instanceof ItemKey key)) {
            return 0;
        }
        final ItemStack stack = container.getItem(index);
        if (stack.isEmpty()) {
            return key.maxStackSize();
        }
        final boolean same = ItemStack.isSameItemSameComponents(stack, key.toStack(1));
        return same ? Math.max(0, stack.getMaxStackSize() - stack.getCount()) : 0;
    }

    @Override
    public long insert(final ResourceKey inserted, final long offered, final Action action) {
        requirePositive(offered);
        final long accepted = Math.min(offered, room(inserted));
        if (accepted > 0 && action.isExecute()) {
            final ItemStack stack = container.getItem(index);
            if (stack.isEmpty()) {
                container.setItem(index, ((ItemKey) inserted).toStack((int) accepted));
            } else {
                stack.grow((int) accepted);
                container.setChanged();
            }
        }
        return accepted;
    }

    @Override
    public long extract(final ResourceKey extracted, final long requested, final Action action) {
        requirePositive(requested);
        final ItemStack stack = container.getItem(index);
        if (stack.isEmpty() || !(extracted instanceof ItemKey key)
                || !ItemStack.isSameItemSameComponents(stack, key.toStack(1))) {
            return 0;
        }
        final long removed = Math.min(requested, stack.getCount());
        if (removed > 0 && action.isExecute()) {
            stack.shrink((int) removed);
            if (stack.isEmpty()) {
                container.setItem(index, ItemStack.EMPTY);
            } else {
                container.setChanged();
            }
        }
        return removed;
    }

    @Override
    public void restore(final @Nullable ResourceKey saved, final long savedAmount) {
        if (saved instanceof ItemKey key && savedAmount > 0) {
            container.setItem(index, key.toStack((int) Math.min(savedAmount, key.maxStackSize())));
        } else {
            container.setItem(index, ItemStack.EMPTY);
        }
    }

    private static void requirePositive(final long units) {
        if (units <= 0) {
            throw new IllegalArgumentException("units must be positive: " + units);
        }
    }
}
