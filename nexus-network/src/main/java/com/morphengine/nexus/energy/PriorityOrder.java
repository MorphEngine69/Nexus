package com.morphengine.nexus.energy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Items ranked the way a network ranks its storages: filled from the highest
 * priority down and drained from the lowest up, so an item given a high
 * priority fills first and keeps its contents longest. Items of equal priority
 * are filled and drained in the order they were given. Immutable.
 *
 * @param <T> the ranked items
 */
public final class PriorityOrder<T> {

    private static final PriorityOrder<?> EMPTY = new PriorityOrder<>(List.of(), List.of());

    private final List<T> fillOrder;
    private final List<T> drainOrder;

    private PriorityOrder(final List<T> fillOrder, final List<T> drainOrder) {
        this.fillOrder = fillOrder;
        this.drainOrder = drainOrder;
    }

    @SuppressWarnings("unchecked")
    public static <T> PriorityOrder<T> empty() {
        return (PriorityOrder<T>) EMPTY;
    }

    /**
     * @return the items all at one priority, filled and drained in list order
     */
    public static <T> PriorityOrder<T> inListOrder(final List<? extends T> items) {
        final List<T> copy = List.copyOf(items);
        return new PriorityOrder<>(copy, copy);
    }

    /**
     * @param ranked the items with their priorities; ties keep this order
     */
    public static <T> PriorityOrder<T> of(final List<Ranked<T>> ranked) {
        Objects.requireNonNull(ranked, "ranked must not be null");
        final List<Ranked<T>> byPriority = new ArrayList<>(ranked);
        byPriority.sort(Comparator.comparingInt((Ranked<T> item) -> item.priority()).reversed());
        final List<T> fill = new ArrayList<>(byPriority.size());
        for (Ranked<T> item : byPriority) {
            fill.add(item.item());
        }
        final List<T> drain = new ArrayList<>(byPriority.size());
        int end = byPriority.size();
        while (end > 0) {
            int start = end - 1;
            while (start > 0 && byPriority.get(start - 1).priority() == byPriority.get(end - 1).priority()) {
                start--;
            }
            drain.addAll(fill.subList(start, end));
            end = start;
        }
        return new PriorityOrder<>(List.copyOf(fill), List.copyOf(drain));
    }

    /**
     * @return every item, highest priority first
     */
    public List<T> fillOrder() {
        return fillOrder;
    }

    /**
     * @return every item, lowest priority first
     */
    public List<T> drainOrder() {
        return drainOrder;
    }

    /**
     * An item and its priority.
     *
     * @param <T> the item
     */
    public record Ranked<T>(T item, int priority) {

        public Ranked {
            Objects.requireNonNull(item, "item must not be null");
        }
    }
}
