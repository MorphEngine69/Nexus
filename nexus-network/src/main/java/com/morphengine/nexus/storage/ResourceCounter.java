package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Amounts of resources kept in the order each resource first appeared, with
 * their sum. A resource whose amount drops to zero is forgotten. Not
 * thread-safe.
 */
public final class ResourceCounter {

    private final Map<ResourceKey, Long> amounts = new LinkedHashMap<>();
    private long total;

    public long amountOf(final ResourceKey resource) {
        return amounts.getOrDefault(resource, 0L);
    }

    /**
     * @return number of distinct resources counted
     */
    public int size() {
        return amounts.size();
    }

    /**
     * @return the sum of all amounts
     */
    public long total() {
        return total;
    }

    /**
     * @param amount units to add, must be positive
     * @return the amount of {@code resource} after the addition
     * @throws ArithmeticException if the amount or the total would overflow a {@code long}
     */
    public long add(final ResourceKey resource, final long amount) {
        Objects.requireNonNull(resource, "resource must not be null");
        requirePositive(resource, amount);
        final long updated = Math.addExact(amountOf(resource), amount);
        total = Math.addExact(total, amount);
        amounts.put(resource, updated);
        return updated;
    }

    /**
     * @param amount units to remove, positive and at most the amount counted
     * @return the amount of {@code resource} left, zero when it is gone
     */
    public long remove(final ResourceKey resource, final long amount) {
        requirePositive(resource, amount);
        final long current = amountOf(resource);
        if (amount > current) {
            throw new IllegalArgumentException(
                    "cannot remove " + amount + " of " + resource + ", only " + current + " counted");
        }
        final long left = current - amount;
        total -= amount;
        if (left == 0) {
            amounts.remove(resource);
        } else {
            amounts.put(resource, left);
        }
        return left;
    }

    /**
     * Forgets every resource.
     */
    public void clear() {
        amounts.clear();
        total = 0;
    }

    /**
     * @return every counted resource with its amount, in first-seen order; a snapshot
     */
    public List<ResourceAmount> contents() {
        final List<ResourceAmount> contents = new ArrayList<>(amounts.size());
        for (Map.Entry<ResourceKey, Long> entry : amounts.entrySet()) {
            contents.add(new ResourceAmount(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(contents);
    }

    private static void requirePositive(final ResourceKey resource, final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount of " + resource + " must be positive: " + amount);
        }
    }
}
