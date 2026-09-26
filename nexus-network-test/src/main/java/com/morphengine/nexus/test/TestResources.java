package com.morphengine.nexus.test;

import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.resource.ResourceType;
import com.morphengine.nexus.api.storage.CellSpec;

/**
 * Resources and cell specs for tests of the core, standing in for items and fluids.
 */
public final class TestResources {

    public static final ResourceType ITEMS = new ResourceType() {
    };
    public static final ResourceType FLUIDS = new ResourceType() {
    };

    public static final ResourceKey STONE = item("stone");
    public static final ResourceKey DIRT = item("dirt");
    public static final ResourceKey SAND = item("sand");
    public static final ResourceKey WATER = new Key("water", FLUIDS);

    /** 64 bytes, 8 per type, at most 4 types, 8 units per byte. */
    public static final CellSpec SMALL = new CellSpec(64, 8, 4, 8);

    private TestResources() {
    }

    public static ResourceKey item(final String name) {
        return new Key(name, ITEMS);
    }

    private record Key(String name, ResourceType type) implements ResourceKey {
    }
}
