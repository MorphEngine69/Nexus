package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.morphengine.nexus.filter.FilterKinds;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.resource.EnergyKey;
import com.morphengine.nexus.resource.NexusResourceType;
import com.morphengine.nexus.resource.ResourceTypes;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The one kind of resource an attached device moves, chosen in its panel. The
 * device keeps a filter for each kind, so switching back finds it as it was.
 * Items and fluids are chosen in the filter; energy has nothing to choose, so
 * its filter is fixed to FE and locked.
 */
public enum TransferResource implements StringRepresentable {

    ITEM(ResourceTypes.ITEM, FilterKinds.ITEMS),
    FLUID(ResourceTypes.FLUID, FilterKinds.FLUIDS),
    ENERGY(ResourceTypes.ENERGY, FilterKinds.NONE);

    public static final Codec<TransferResource> CODEC = StringRepresentable.fromEnum(TransferResource::values);
    /** Every kind, as a Puller or Pusher moves them. */
    public static final List<TransferResource> ALL = List.of(values());
    /** Items and fluids, what a Placer or Remover finds in the world. */
    public static final List<TransferResource> MATERIAL = List.of(ITEM, FLUID);

    private final Supplier<? extends NexusResourceType<?>> resourceType;
    private final FilterKinds filterKinds;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    TransferResource(final Supplier<? extends NexusResourceType<?>> resourceType, final FilterKinds filterKinds) {
        this.resourceType = resourceType;
        this.filterKinds = filterKinds;
    }

    public NexusResourceType<?> resourceType() {
        return resourceType.get();
    }

    /**
     * @return what the player lists in the filter; {@link FilterKinds#NONE} for energy
     */
    public FilterKinds filterKinds() {
        return filterKinds;
    }

    /**
     * @return {@code filter} made fit for this resource: for energy a whitelist
     *         of FE alone in the first slot; otherwise the same filter without
     *         entries of other kinds
     */
    FilterSlots normalize(final FilterSlots filter) {
        if (this == ENERGY) {
            return FilterSlots.EMPTY.with(0, EnergyKey.INSTANCE);
        }
        final List<FilterSlots.Entry> kept = new ArrayList<>(filter.entries().size());
        for (FilterSlots.Entry entry : filter.entries()) {
            if (entry.resource().type() == resourceType()) {
                kept.add(entry);
            }
        }
        return new FilterSlots(filter.mode(), kept);
    }

    /**
     * The resource of a device saved before it had one: what its filter lists
     * first, items when it lists nothing.
     */
    static TransferResource inferredFrom(final FilterSlots filter) {
        final List<FilterSlots.Entry> listed = filter.inSlotOrder();
        if (!listed.isEmpty()) {
            for (TransferResource resource : values()) {
                if (resource.resourceType() == listed.getFirst().resource().type()) {
                    return resource;
                }
            }
        }
        return ITEM;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
