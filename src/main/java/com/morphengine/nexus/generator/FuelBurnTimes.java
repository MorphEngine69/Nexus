package com.morphengine.nexus.generator;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

import java.util.Optional;

/**
 * Reads how long an item burns from the cooking fuel component, as a furnace does.
 */
final class FuelBurnTimes {

    private FuelBurnTimes() {
    }

    static int of(final ServerLevel level, final ItemStack stack) {
        final LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        final LootContext context = new LootContext.Builder(params).create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
    }
}
