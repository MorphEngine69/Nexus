package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.resource.ResourceType;
import com.morphengine.nexus.api.storage.CellSpec;

/**
 * Resources and cell specs for storage tests, standing in for items and fluids.
 */
final class TestResources {

    static final ResourceType ITEMS = new ResourceType() {
    };
    static final ResourceType FLUIDS = new ResourceType() {
    };

    static final ResourceKey STONE = item("stone");
    static final ResourceKey DIRT = item("dirt");
    static final ResourceKey SAND = item("sand");
    static final ResourceKey WATER = new Key("water", FLUIDS);

    /** 64 bytes, 8 per type, at most 4 types, 8 units per byte. */
    static final CellSpec SMALL = new CellSpec(64, 8, 4, 8);

    private TestResources() {
    }

    static ResourceKey item(final String name) {
        return new Key(name, ITEMS);
    }

    private record Key(String name, ResourceType type) implements ResourceKey {
    }
}
