package com.morphengine.nexus.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The fluid a machine or a generator held when it was taken down, kept on its item so that it comes back when the
 * block is put up again. One entry for each tank, in the order of the tanks, an empty stack for a tank that held
 * nothing.
 *
 * @param tanks what each tank held, in millibuckets; copied
 */
public record StoredFluids(List<FluidStack> tanks) {

    public static final StoredFluids EMPTY = new StoredFluids(List.of());

    public static final Codec<StoredFluids> CODEC =
            FluidStack.OPTIONAL_CODEC.listOf().xmap(StoredFluids::new, StoredFluids::tanks);

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredFluids> STREAM_CODEC =
            FluidStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).map(StoredFluids::new, StoredFluids::tanks);

    public StoredFluids {
        Objects.requireNonNull(tanks, "tanks must not be null");
        final List<FluidStack> copies = new ArrayList<>(tanks.size());
        for (FluidStack tank : tanks) {
            copies.add(tank.copy());
        }
        tanks = List.copyOf(copies);
    }

    /**
     * @return whether no tank held anything
     */
    public boolean isEmpty() {
        return tanks.stream().allMatch(FluidStack::isEmpty);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof StoredFluids that) || that.tanks.size() != tanks.size()) {
            return false;
        }
        for (int index = 0; index < tanks.size(); index++) {
            if (!FluidStack.matches(tanks.get(index), that.tanks.get(index))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return tanks.stream().mapToInt(FluidStack::hashFluidAndComponents)
                .reduce(1, (hash, next) -> Objects.hash(hash, next));
    }
}
