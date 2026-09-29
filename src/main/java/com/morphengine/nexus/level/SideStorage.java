package com.morphengine.nexus.level;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.resource.EnergyKey;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * The items, fluids and energy a block offers through one of its sides, as a
 * {@link Storage}. Only the slots the block exposes on that side are seen, so
 * a furnace shows its input from above and its result from below, and a block
 * whose sides the player configures follows that configuration. Each operation
 * runs in a transaction of its own; the block's handlers are read live. Server
 * thread only.
 */
public final class SideStorage implements Storage {

    private final @Nullable ResourceHandler<ItemResource> items;
    private final @Nullable ResourceHandler<FluidResource> fluids;
    private final @Nullable EnergyHandler energy;

    /**
     * @param items  the block's items on that side; {@code null} when it has none
     * @param fluids the block's fluids on that side; {@code null} when it has none
     * @param energy the block's energy on that side; {@code null} when it has none
     */
    public SideStorage(
            final @Nullable ResourceHandler<ItemResource> items,
            final @Nullable ResourceHandler<FluidResource> fluids,
            final @Nullable EnergyHandler energy) {
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
        final int offered = clamp(amount);
        return switch (resource) {
            case ItemKey item when items != null ->
                inTransaction(action, transaction -> items.insert(item.item(), offered, transaction));
            case FluidKey fluid when fluids != null ->
                inTransaction(action, transaction -> fluids.insert(fluid.fluid(), offered, transaction));
            case EnergyKey _ when energy != null ->
                inTransaction(action, transaction -> energy.insert(offered, transaction));
            default -> 0;
        };
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        checkArguments(resource, action, actor);
        final int wanted = clamp(amount);
        return switch (resource) {
            case ItemKey item when items != null ->
                inTransaction(action, transaction -> items.extract(item.item(), wanted, transaction));
            case FluidKey fluid when fluids != null ->
                inTransaction(action, transaction -> fluids.extract(fluid.fluid(), wanted, transaction));
            case EnergyKey _ when energy != null ->
                inTransaction(action, transaction -> energy.extract(wanted, transaction));
            default -> 0;
        };
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return switch (resource) {
            case ItemKey item when items != null -> amountIn(items, item.item());
            case FluidKey fluid when fluids != null -> amountIn(fluids, fluid.fluid());
            case EnergyKey _ when energy != null -> energy.getAmountAsLong();
            default -> 0;
        };
    }

    /**
     * @return the items, then the fluids, each once, in the order of the
     *         block's slots, then the energy when there is any; a snapshot
     */
    @Override
    public List<ResourceAmount> contents() {
        final List<ResourceAmount> contents = new ArrayList<>();
        if (items != null) {
            contents.addAll(contentsOf(items, ItemKey::new));
        }
        if (fluids != null) {
            contents.addAll(contentsOf(fluids, FluidKey::new));
        }
        final long stored = energy != null ? energy.getAmountAsLong() : 0;
        if (stored > 0) {
            contents.add(new ResourceAmount(EnergyKey.INSTANCE, stored));
        }
        return List.copyOf(contents);
    }

    /**
     * Runs {@code operation} in a transaction of its own, kept only for
     * {@link Action#EXECUTE}.
     *
     * @return what the operation moved
     */
    private static long inTransaction(final Action action, final ToIntFunction<Transaction> operation) {
        try (Transaction transaction = Transaction.openRoot()) {
            final int moved = operation.applyAsInt(transaction);
            if (action.isExecute()) {
                transaction.commit();
            }
            return moved;
        }
    }

    private static <T extends Resource> long amountIn(final ResourceHandler<T> handler, final T resource) {
        long total = 0;
        for (int index = 0; index < handler.size(); index++) {
            if (handler.getResource(index).equals(resource)) {
                total += handler.getAmountAsLong(index);
            }
        }
        return total;
    }

    /**
     * @return what {@code handler} holds, each resource once, in the order of its slots
     */
    private static <T extends Resource> List<ResourceAmount> contentsOf(
            final ResourceHandler<T> handler, final Function<T, ResourceKey> keyOf) {
        final Map<ResourceKey, Long> totals = new LinkedHashMap<>();
        for (int index = 0; index < handler.size(); index++) {
            final T resource = handler.getResource(index);
            final long amount = handler.getAmountAsLong(index);
            if (!resource.isEmpty() && amount > 0) {
                totals.merge(keyOf.apply(resource), amount, Long::sum);
            }
        }
        final List<ResourceAmount> contents = new ArrayList<>(totals.size());
        for (Map.Entry<ResourceKey, Long> total : totals.entrySet()) {
            contents.add(new ResourceAmount(total.getKey(), total.getValue()));
        }
        return contents;
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
