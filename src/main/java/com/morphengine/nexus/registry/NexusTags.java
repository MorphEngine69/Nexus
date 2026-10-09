package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class NexusTags {

    /** Items the Coal Generator burns: coal, charcoal and coal blocks. */
    public static final TagKey<Item> COAL_GENERATOR_FUELS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "coal_generator_fuels"));

    private NexusTags() {
    }
}
