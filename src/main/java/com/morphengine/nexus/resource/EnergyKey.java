package com.morphengine.nexus.resource;

import com.mojang.serialization.MapCodec;
import com.morphengine.nexus.Nexus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Energy, the one resource of its kind: every FE is alike, so all keys are
 * equal. Amounts of it are in FE.
 */
public record EnergyKey() implements NexusResource {

    public static final EnergyKey INSTANCE = new EnergyKey();

    public static final MapCodec<EnergyKey> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyKey> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private static final Identifier ID = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "energy");

    @Override
    public NexusResourceType<?> type() {
        return ResourceTypes.ENERGY.get();
    }

    @Override
    public Component name() {
        return Component.translatable("resource.nexus.energy");
    }

    @Override
    public Identifier id() {
        return ID;
    }
}
