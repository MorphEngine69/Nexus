package com.morphengine.nexus.generator;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;

/**
 * What a generator burns: pieces of an item that go in its fuel slot, or portions of fluid that it draws out of its
 * tanks. A new way to burn is a new case here and in the generator that reads it.
 */
public sealed interface GeneratorFuel {

    /**
     * @return the tanks the generator needs, in the order of its panel; none for a generator that burns items
     */
    List<TankSpec> tanks();

    /**
     * Items in the fuel slot, burnt one at a time.
     *
     * @param accepts   whether an item is fuel
     * @param burnTicks how long an item burns in a level, positive
     */
    record ItemFuel(Predicate<ItemStack> accepts, ToIntBiFunction<ServerLevel, ItemStack> burnTicks)
            implements GeneratorFuel {

        public ItemFuel {
            Objects.requireNonNull(accepts, "accepts");
            Objects.requireNonNull(burnTicks, "burnTicks");
        }

        @Override
        public List<TankSpec> tanks() {
            return List.of();
        }
    }

    /**
     * Fluids in tanks, burnt a portion at a time: a portion takes {@link TankSpec#portionMillibuckets()} out of every
     * tank, and burns for the same time whatever the fluids are.
     *
     * @param burnTicksPerPortion how long a portion burns, positive
     */
    record FluidFuel(List<TankSpec> tanks, int burnTicksPerPortion) implements GeneratorFuel {

        public FluidFuel {
            tanks = List.copyOf(tanks);
            if (tanks.isEmpty() || burnTicksPerPortion <= 0) {
                throw new IllegalArgumentException("a fluid fuel needs a tank and a burn time: " + tanks.size()
                        + " tanks, " + burnTicksPerPortion + " ticks");
            }
        }
    }
}
