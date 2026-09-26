package com.morphengine.nexus.level;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import net.neoforged.neoforge.transfer.ResourceHandler;
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

/**
 * The items and fluids a block offers through one of its sides, as a
 * {@link Storage}. Only the slots the block exposes on that side are seen, so
 * a furnace shows its input from above and its result from below, and a block
 * whose sides the player configures follows that configuration. Each operation
 * runs in a transaction of its own; the block's handlers are read live. Server
 * thread only.
 */
public final class SideStorage implements Storage {

    private final @Nullable ResourceHandler<ItemResource> items;
    private final @Nullable ResourceHandler<FluidResource> fluids;

    /**
     * @param items  the block's items on that side; {@code null} when it has none
     * @param fluids the block's fluids on that side; {@code null} when it has none
     */
    public SideStorage(
            final @Nullable ResourceHandler<ItemResource> items,
            final @Nullable ResourceHandler<FluidResource> fluids) {
        this.items = items;
        this.fluids = fluids;
    }

    /**
     * @return whether the block offers anything on that side
     */
    public boolean isPresent() {
        return items != null || fluids != null;
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        checkArguments(resource, action, actor);
        return switch (resource) {
            case ItemKey item when items != null -> insertInto(items, item.item(), amount, action);
            case FluidKey fluid when fluids != null -> insertInto(fluids, fluid.fluid(), amount, action);
            default -> 0;
        };
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        checkArguments(resource, action, actor);
        return switch (resource) {
            case ItemKey item when items != null -> extractFrom(items, item.item(), amount, action);
            case FluidKey fluid when fluids != null -> extractFrom(fluids, fluid.fluid(), amount, action);
            default -> 0;
        };
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return switch (resource) {
            case ItemKey item when items != null -> amountIn(items, item.item());
            case FluidKey fluid when fluids != null -> amountIn(fluids, fluid.fluid());
            default -> 0;
        };
    }

    /**
     * @return the items, then the fluids, each once, in the order of the
     *         block's slots; a snapshot
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
        return List.copyOf(contents);
    }

    private static <T extends Resource> long insertInto(
            final ResourceHandler<T> handler, final T resource, final long amount, final Action action) {
        try (Transaction transaction = Transaction.openRoot()) {
            final int inserted = handler.insert(resource, clamp(amount), transaction);
            if (action.isExecute()) {
                transaction.commit();
            }
            return inserted;
        }
    }

    private static <T extends Resource> long extractFrom(
            final ResourceHandler<T> handler, final T resource, final long amount, final Action action) {
        try (Transaction transaction = Transaction.openRoot()) {
            final int extracted = handler.extract(resource, clamp(amount), transaction);
            if (action.isExecute()) {
                transaction.commit();
            }
            return extracted;
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
