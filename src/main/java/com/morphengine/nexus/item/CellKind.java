package com.morphengine.nexus.item;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.CellSpec;
import com.morphengine.nexus.api.storage.CellUsage;
import com.morphengine.nexus.api.storage.StorageCell;
import com.morphengine.nexus.filter.FilterKinds;
import com.morphengine.nexus.math.SaturatedMath;
import com.morphengine.nexus.resource.EnergyKey;
import com.morphengine.nexus.resource.NexusResourceType;
import com.morphengine.nexus.resource.ResourceTypes;
import com.morphengine.nexus.storage.CellStorage;
import com.morphengine.nexus.storage.SingleResourceCell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What a line of Vault Cells stores. Placeholder balance until the numbers are
 * settled: 64 item types like the 63 of AE2, 8 fluid types; a byte holds 8
 * items, 8 buckets or 8192 FE, so energy cells run from 8 million FE at 1k to
 * 4 billion at 512k.
 */
public enum CellKind implements StringRepresentable {

    ITEM(64, 8, ResourceTypes.ITEM, FilterKinds.ITEMS),
    FLUID(8, 8L * FluidType.BUCKET_VOLUME, ResourceTypes.FLUID, FilterKinds.FLUIDS),

    /** FE only, so the cell has no filter and no types to run out of. */
    ENERGY(1, 8192, ResourceTypes.ENERGY, FilterKinds.NONE) {
        @Override
        public StorageCell createStorage(final CellSpec spec, final List<ResourceAmount> contents) {
            return new SingleResourceCell(EnergyKey.INSTANCE, spec, contents);
        }

        @Override
        public void describe(final StorageCell cell, final Consumer<Component> lines) {
            final long capacity = SaturatedMath.multiply(cell.usage().totalBytes(), unitsPerByte());
            lines.accept(Component.translatable("tooltip.nexus.cell.energy",
                    grouped(cell.amountOf(EnergyKey.INSTANCE)), grouped(capacity)).withStyle(ChatFormatting.GRAY));
        }
    };

    private final int maxTypes;
    private final long unitsPerByte;
    private final Supplier<? extends NexusResourceType<?>> resourceType;
    private final FilterKinds filterKinds;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    CellKind(
            final int maxTypes, final long unitsPerByte, final Supplier<? extends NexusResourceType<?>> resourceType,
            final FilterKinds filterKinds) {
        this.maxTypes = maxTypes;
        this.unitsPerByte = unitsPerByte;
        this.resourceType = resourceType;
        this.filterKinds = filterKinds;
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

    /**
     * @return what the filter of such a cell lists: the one kind the cell
     *         stores, or {@link FilterKinds#NONE} when such cells have no filter
     */
    public FilterKinds filterKinds() {
        return filterKinds;
    }

    public boolean hasFilter() {
        return filterKinds.listsAnything();
    }

    /**
     * @param contents what the cell held when it was saved
     * @return a storage of this kind over {@code contents}
     */
    public StorageCell createStorage(final CellSpec spec, final List<ResourceAmount> contents) {
        return new CellStorage(resourceType(), spec, contents);
    }

    /**
     * Tells how full {@code cell} is, as the lines of its tooltip.
     */
    public void describe(final StorageCell cell, final Consumer<Component> lines) {
        final CellUsage usage = cell.usage();
        lines.accept(Component.translatable("tooltip.nexus.cell.bytes",
                grouped(usage.usedBytes()), grouped(usage.totalBytes())).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.nexus.cell.types",
                usage.storedTypes(), usage.maxTypes()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    private static String grouped(final long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
