package com.morphengine.nexus.test;

import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.resource.ResourceType;
import com.morphengine.nexus.api.storage.CellSpec;

/**
 * Resources and cell specs for tests of the core, standing in for items, fluids and energy.
 */
public final class TestResources {

    public static final ResourceType ITEMS = new ResourceType() {
    };
    public static final ResourceType FLUIDS = new ResourceType() {
    };
    public static final ResourceType ENERGY_TYPE = new ResourceType() {
    };

    public static final ResourceKey STONE = item("stone");
    public static final ResourceKey DIRT = item("dirt");
    public static final ResourceKey SAND = item("sand");
    public static final ResourceKey WATER = new Key("water", FLUIDS);
    /** The one resource of {@link #ENERGY_TYPE}. */
    public static final ResourceKey ENERGY = new Key("energy", ENERGY_TYPE);

    /** 64 bytes, 8 per type, at most 4 types, 8 units per byte. */
    public static final CellSpec SMALL = new CellSpec(64, 8, 4, 8);

    /** 4 bytes of 100 units each: 400 units for a single resource. */
    public static final CellSpec ENERGY_CELL = new CellSpec(4, 1, 1, 100);

    private TestResources() {
    }

    public static ResourceKey item(final String name) {
        return new Key(name, ITEMS);
    }

    private record Key(String name, ResourceType type) implements ResourceKey {
    }
}
