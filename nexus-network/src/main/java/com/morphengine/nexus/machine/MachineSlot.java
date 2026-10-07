package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A slot of a machine that holds one kind of resource at a time. Where the contents live is up to the slot: a
 * {@link PlainMachineSlot} keeps them itself, a slot of a game can show a stack that a menu changes in place. Server
 * thread only.
 */
public interface MachineSlot {

    /**
     * @return what the slot holds, {@code null} when it is empty
     */
    @Nullable ResourceKey resource();

    long amount();

    default boolean isEmpty() {
        return resource() == null;
    }

    /**
     * @return what the slot holds with the amount, empty when it is empty
     */
    default Optional<ResourceAmount> contents() {
        final ResourceKey held = resource();
        return held == null ? Optional.empty() : Optional.of(new ResourceAmount(held, amount()));
    }

    /**
     * @return how many units of {@code wanted} the slot takes now: the limit less what is there when it holds
     *         {@code wanted}, the whole limit when it is empty, zero when it holds something else
     */
    long room(ResourceKey wanted);

    /**
     * @param offered units offered, must be positive
     * @return units accepted, never more than {@link #room}; under {@link Action#SIMULATE} the units that would be
     */
    long insert(ResourceKey inserted, long offered, Action action);

    /**
     * @param requested units wanted, must be positive
     * @return units removed, none when the slot holds another resource; under {@link Action#SIMULATE} the units that
     *         would be
     */
    long extract(ResourceKey extracted, long requested, Action action);

    /**
     * Puts saved contents back; an amount of zero or less empties the slot.
     */
    void restore(@Nullable ResourceKey saved, long savedAmount);
}
