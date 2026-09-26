package com.morphengine.nexus.item;

import com.morphengine.nexus.resource.NexusResourceType;
import com.morphengine.nexus.resource.ResourceTypes;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * What a line of Vault Cells stores. Placeholder balance until the numbers are
 * settled: 64 item types like the 63 of AE2, 8 fluid types; a byte holds 8 items
 * or 8 buckets.
 */
public enum CellKind implements StringRepresentable {

    ITEM(64, 8, ResourceTypes.ITEM),
    FLUID(8, 8L * FluidType.BUCKET_VOLUME, ResourceTypes.FLUID);

    private final int maxTypes;
    private final long unitsPerByte;
    private final Supplier<? extends NexusResourceType<?>> resourceType;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    CellKind(final int maxTypes, final long unitsPerByte, final Supplier<? extends NexusResourceType<?>> resourceType) {
        this.maxTypes = maxTypes;
        this.unitsPerByte = unitsPerByte;
        this.resourceType = resourceType;
    }

    public int maxTypes() {
        return maxTypes;
    }

    public long unitsPerByte() {
        return unitsPerByte;
    }

    public NexusResourceType<?> resourceType() {
        return resourceType.get();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
