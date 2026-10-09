package com.morphengine.nexus.gametest;

import com.morphengine.nexus.transfer.FluidResource;
import com.morphengine.nexus.transfer.ItemResource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

/**
 * An item or fluid handler whose changes belong to a {@link Transaction}.
 *
 * @param <R> the kind of resource it holds
 */
interface ResourceHandler<R> {

    int insert(R resource, int amount, Transaction transaction);

    int extract(R resource, int amount, Transaction transaction);

    int getAmountAsInt(int slot);

    R getResource(int slot);

    static @Nullable ResourceHandler<ItemResource> ofItems(final @Nullable IItemHandler items) {
        return items == null ? null : new Items(items);
    }

    static @Nullable ResourceHandler<FluidResource> ofFluids(final @Nullable IFluidHandler fluids) {
        return fluids == null ? null : new Fluids(fluids);
    }

    record Items(IItemHandler items) implements ResourceHandler<ItemResource> {

        @Override
        public int insert(final ItemResource resource, final int amount, final Transaction transaction) {
            int left = amount;
            for (int slot = 0; slot < items.getSlots() && left > 0; slot++) {
                final int moved = left - items.insertItem(slot, resource.toStack(left), false).getCount();
                left -= moved;
                if (moved > 0) {
                    final int slotIndex = slot;
                    transaction.onRollback(() -> items.extractItem(slotIndex, moved, false));
                }
            }
            return amount - left;
        }

        @Override
        public int extract(final ItemResource resource, final int amount, final Transaction transaction) {
            int left = amount;
            for (int slot = 0; slot < items.getSlots() && left > 0; slot++) {
                if (!resource.matches(items.getStackInSlot(slot))) {
                    continue;
                }
                final ItemStack taken = items.extractItem(slot, left, false);
                left -= taken.getCount();
                final int slotIndex = slot;
                transaction.onRollback(() -> items.insertItem(slotIndex, taken, false));
            }
            return amount - left;
        }

        @Override
        public int getAmountAsInt(final int slot) {
            return items.getStackInSlot(slot).getCount();
        }

        @Override
        public ItemResource getResource(final int slot) {
            return ItemResource.of(items.getStackInSlot(slot));
        }
    }

    record Fluids(IFluidHandler fluids) implements ResourceHandler<FluidResource> {

        @Override
        public int insert(final FluidResource resource, final int amount, final Transaction transaction) {
            final int accepted = fluids.fill(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
            transaction.onRollback(
                    () -> fluids.drain(resource.toStack(accepted), IFluidHandler.FluidAction.EXECUTE));
            return accepted;
        }

        @Override
        public int extract(final FluidResource resource, final int amount, final Transaction transaction) {
            final int removed = fluids.drain(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE)
                    .getAmount();
            transaction.onRollback(() -> fluids.fill(resource.toStack(removed), IFluidHandler.FluidAction.EXECUTE));
            return removed;
        }

        @Override
        public int getAmountAsInt(final int tank) {
            return fluids.getFluidInTank(tank).getAmount();
        }

        @Override
        public FluidResource getResource(final int tank) {
            return FluidResource.of(fluids.getFluidInTank(tank));
        }
    }
}
