package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.energy.SimpleEnergyBuffer;
import com.morphengine.nexus.item.StoredFluids;
import com.morphengine.nexus.registry.NexusDataComponents;
import net.minecraft.core.component.DataComponentMap;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The FE and the fluid a machine or a generator holds, carried on its item when it is taken down, so that they are
 * still there when it is put up again.
 */
public final class StoredContents implements ItemComponentPart {

    private final SimpleEnergyBuffer energy;
    private final @Nullable FluidKeeper fluids;

    /**
     * @param energy the buffer of the device, shared with it
     * @param fluids the tanks of the device, shared with it; {@code null} for a device without tanks
     */
    public StoredContents(final SimpleEnergyBuffer energy, final @Nullable FluidKeeper fluids) {
        this.energy = Objects.requireNonNull(energy, "energy must not be null");
        this.fluids = fluids;
    }

    @Override
    public void collectInto(final DataComponentMap.Builder components) {
        components.set(NexusDataComponents.STORED_ENERGY.get(), energy.stored());
        if (fluids != null) {
            components.set(NexusDataComponents.STORED_FLUIDS.get(), new StoredFluids(fluids.held()));
        }
    }

    @Override
    public void applyFrom(final ComponentSource components) {
        final long stored = components.getOrDefault(NexusDataComponents.STORED_ENERGY.get(), 0L);
        energy.restore(SimpleEnergyBuffer.Snapshot.storing(Math.clamp(stored, 0, energy.capacity())));
        if (fluids != null) {
            fluids.restore(components.getOrDefault(NexusDataComponents.STORED_FLUIDS.get(),
                    StoredFluids.EMPTY).tanks());
        }
    }
}
