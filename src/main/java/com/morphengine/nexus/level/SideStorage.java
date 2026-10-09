package com.morphengine.nexus.level;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.resource.EnergyKey;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.transfer.FluidResource;
import com.morphengine.nexus.transfer.ItemResource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The items, fluids and energy a block offers through one of its sides, as a
 * {@link Storage}. Only the slots the block exposes on that side are seen, so
 * a furnace shows its input from above and its result from below, and a block
 * whose sides the player configures follows that configuration. The handlers
 * of the block are read live. Server thread only.
 */
public final class SideStorage implements Storage {

    private final @Nullable IItemHandler items;
    private final @Nullable IFluidHandler fluids;
    private final @Nullable IEnergyStorage energy;

    /**
     * @param items  the items of the block on that side; {@code null} when it has none
     * @param fluids the fluids of the block on that side; {@code null} when it has none
     * @param energy the energy of the block on that side; {@code null} when it has none
     */
    public SideStorage(
            final @Nullable IItemHandler items,
            final @Nullable IFluidHandler fluids,
            final @Nullable IEnergyStorage energy) {
        this.items = items;
        this.fluids = fluids;
        this.energy = energy;
    }

    /**
     * @return whether the block offers anything on that side
     */
    public boolean isPresent() {
        return items != null || fluids != null || energy != null;
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        checkArguments(resource, action, actor);
        return insertInto(resource, clamp(amount), action);
    }

    /**
     * Inserts every amount in full or nothing at all, so the inputs of a machine never arrive half. All are
     * simulated first; they are then inserted, and whatever fell short is taken back.
     *
     * @return whether everything fits; under {@link Action#SIMULATE} whether it would
     */
    public boolean insertAll(final List<ResourceAmount> amounts, final Action action) {
        for (ResourceAmount amount : amounts) {
            if (insertInto(amount.resource(), clamp(amount.amount()), Action.SIMULATE) < amount.amount()) {
                return false;
            }
        }
        if (action.isExecute()) {
            final List<ResourceAmount> inserted = new ArrayList<>(amounts.size());
            for (ResourceAmount amount : amounts) {
                final long moved = insertInto(amount.resource(), clamp(amount.amount()), Action.EXECUTE);
                inserted.add(new ResourceAmount(amount.resource(), moved));
                if (moved < amount.amount()) {
                    takeBack(inserted);
                    return false;
                }
            }
        }
        return true;
    }

    private void takeBack(final List<ResourceAmount> inserted) {
        for (ResourceAmount amount : inserted) {
            if (amount.amount() > 0) {
                extractFrom(amount.resource(), clamp(amount.amount()), Action.EXECUTE);
            }
        }
    }

    private long insertInto(final ResourceKey resource, final int offered, final Action action) {
        final boolean simulate = !action.isExecute();
        return switch (resource) {
            case ItemKey item when items != null -> insertItems(item.item(), offered, simulate);
            case FluidKey fluid when fluids != null ->
                fluids.fill(fluid.fluid().toStack(offered), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
            case EnergyKey ignored when energy != null -> energy.receiveEnergy(offered, simulate);
            default -> 0;
        };
    }

    private int insertItems(final ItemResource resource, final int amount, final boolean simulate) {
        if (items == null || amount <= 0) {
            return 0;
        }
        final ItemStack remainder = ItemHandlerHelper.insertItemStacked(items, resource.toStack(amount), simulate);
        return amount - remainder.getCount();
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        checkArguments(resource, action, actor);
        return extractFrom(resource, clamp(amount), action);
    }

    private long extractFrom(final ResourceKey resource, final int wanted, final Action action) {
        final boolean simulate = !action.isExecute();
        return switch (resource) {
            case ItemKey item when items != null -> extractItems(item.item(), wanted, simulate);
            case FluidKey fluid when fluids != null ->
                fluids.drain(fluid.fluid().toStack(wanted), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE)
                        .getAmount();
            case EnergyKey ignored when energy != null -> energy.extractEnergy(wanted, simulate);
            default -> 0;
        };
    }

    private int extractItems(final ItemResource resource, final int wanted, final boolean simulate) {
        if (items == null) {
            return 0;
        }
        int taken = 0;
        for (int slot = 0; slot < items.getSlots() && taken < wanted; slot++) {
            if (resource.matches(items.getStackInSlot(slot))) {
                taken += items.extractItem(slot, wanted - taken, simulate).getCount();
            }
        }
        return taken;
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return switch (resource) {
            case ItemKey item when items != null -> amountOfItem(item.item());
            case FluidKey fluid when fluids != null -> amountOfFluid(fluid.fluid());
            case EnergyKey ignored when energy != null -> energy.getEnergyStored();
            default -> 0;
        };
    }

    private long amountOfItem(final ItemResource resource) {
        long total = 0;
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                final ItemStack stack = items.getStackInSlot(slot);
                if (resource.matches(stack)) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    private long amountOfFluid(final FluidResource resource) {
        long total = 0;
        if (fluids != null) {
            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                final FluidStack stack = fluids.getFluidInTank(tank);
                if (resource.matches(stack)) {
                    total += stack.getAmount();
                }
            }
        }
        return total;
    }

    /**
     * @return the items, then the fluids, each once, in the order of the
     *         slots of the block, then the energy when there is any; a snapshot
     */
    @Override
    public List<ResourceAmount> contents() {
        final Map<ResourceKey, Long> totals = new LinkedHashMap<>();
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                final ItemStack stack = items.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    totals.merge(ItemKey.of(stack), (long) stack.getCount(), Long::sum);
                }
            }
        }
        if (fluids != null) {
            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                final FluidStack stack = fluids.getFluidInTank(tank);
                if (!stack.isEmpty()) {
                    totals.merge(new FluidKey(FluidResource.of(stack)), (long) stack.getAmount(), Long::sum);
                }
            }
        }
        final List<ResourceAmount> contents = new ArrayList<>(totals.size() + 1);
        for (Map.Entry<ResourceKey, Long> total : totals.entrySet()) {
            contents.add(new ResourceAmount(total.getKey(), total.getValue()));
        }
        final long stored = energy != null ? energy.getEnergyStored() : 0;
        if (stored > 0) {
            contents.add(new ResourceAmount(EnergyKey.INSTANCE, stored));
        }
        return List.copyOf(contents);
    }

    private static int clamp(final long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    private static void checkArguments(final ResourceKey resource, final Action action, final Actor actor) {
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
    }
}
