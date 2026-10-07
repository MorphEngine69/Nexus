package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.security.AccessGate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A storage whose energy lives in an {@link EnergyBuffer}: inserts, extracts and
 * the amount of the energy resource go to the buffer, every other resource to
 * the storage. This is how a network shows its whole energy pool, Energy Cells
 * included, as one resource next to its items and fluids. Whatever energy the
 * storage itself holds is left out of its contents, since the buffer is expected
 * to count it already.
 *
 * <p>A gate decides inserts and extracts of the energy, by who they are for,
 * as the energy buffer has no say of its own; every other resource is left to
 * the storage, which guards itself where it needs to. Server thread only.
 */
public final class EnergyBackedStorage implements Storage {

    private final Storage storage;
    private final EnergyBuffer energy;
    private final ResourceKey energyKey;
    private final AccessGate gate;

    /**
     * @param storage   shared with its owner, read live
     * @param energy    shared with its owner, read live
     * @param energyKey the resource that stands for energy
     * @param gate      decides inserts by {@link Permission#INSERT} and extracts by
     *                  {@link Permission#EXTRACT} of the energy
     */
    public EnergyBackedStorage(
            final Storage storage, final EnergyBuffer energy, final ResourceKey energyKey, final AccessGate gate) {
        this.storage = Objects.requireNonNull(storage, "storage must not be null");
        this.energy = Objects.requireNonNull(energy, "energy must not be null");
        this.energyKey = Objects.requireNonNull(energyKey, "energyKey must not be null");
        this.gate = Objects.requireNonNull(gate, "gate must not be null");
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        if (!resource.equals(energyKey)) {
            return storage.insert(resource, amount, action, actor);
        }
        return gate.permits(actor, Permission.INSERT) ? energy.insert(amount, action) : 0;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        StorageArguments.check(resource, amount, action, actor);
        if (!resource.equals(energyKey)) {
            return storage.extract(resource, amount, action, actor);
        }
        return gate.permits(actor, Permission.EXTRACT) ? energy.extract(amount, action) : 0;
    }

    @Override
    public long amountOf(final ResourceKey resource) {
        return resource.equals(energyKey) ? energy.stored() : storage.amountOf(resource);
    }

    /**
     * @return the storage's contents in its order without energy, then the
     *         energy the buffer stores when there is any; a snapshot
     */
    @Override
    public List<ResourceAmount> contents() {
        final List<ResourceAmount> held = storage.contents();
        final List<ResourceAmount> contents = new ArrayList<>(held.size() + 1);
        for (ResourceAmount amount : held) {
            if (!amount.resource().equals(energyKey)) {
                contents.add(amount);
            }
        }
        final long stored = energy.stored();
        if (stored > 0) {
            contents.add(new ResourceAmount(energyKey, stored));
        }
        return List.copyOf(contents);
    }

    @Override
    public boolean isReservedFor(final ResourceKey resource) {
        return storage.isReservedFor(resource);
    }
}
