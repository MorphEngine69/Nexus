package com.morphengine.nexus.registry;

import com.mojang.serialization.Codec;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.blueprint.EncodedBlueprint;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.item.CellContents;
import com.morphengine.nexus.item.StoredFluids;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Supplier;

public final class NexusDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nexus.MOD_ID);

    /** What a Vault Cell holds. */
    public static final Supplier<DataComponentType<CellContents>> CELL_CONTENTS = COMPONENTS.registerComponentType(
            "cell_contents",
            builder -> builder.persistent(CellContents.CODEC).networkSynchronized(CellContents.STREAM_CODEC));

    /** The whitelist or blacklist set on a Vault Cell. */
    public static final Supplier<DataComponentType<FilterSlots>> CELL_FILTER = COMPONENTS.registerComponentType(
            "cell_filter",
            builder -> builder.persistent(FilterSlots.CODEC).networkSynchronized(FilterSlots.STREAM_CODEC));

    /** The resources a Void Upgrade has the network destroy; an empty list destroys nothing. */
    public static final Supplier<DataComponentType<FilterSlots>> DISCARD_FILTER = COMPONENTS.registerComponentType(
            "discard_filter",
            builder -> builder.persistent(FilterSlots.CODEC).networkSynchronized(FilterSlots.STREAM_CODEC));

    /** The FE a machine or a generator held when it was taken down. */
    public static final Supplier<DataComponentType<Long>> STORED_ENERGY = COMPONENTS.registerComponentType(
            "stored_energy",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** The FE a Nexus Terminal holds; opening it spends some. */
    public static final Supplier<DataComponentType<Integer>> TERMINAL_CHARGE = COMPONENTS.registerComponentType(
            "terminal_charge",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The fluid a machine or a generator held in its tanks when it was taken down. */
    public static final Supplier<DataComponentType<StoredFluids>> STORED_FLUIDS = COMPONENTS.registerComponentType(
            "stored_fluids",
            builder -> builder.persistent(StoredFluids.CODEC).networkSynchronized(StoredFluids.STREAM_CODEC));

    /** The recipe encoded on a Blueprint; a blank Blueprint has none. */
    public static final Supplier<DataComponentType<EncodedBlueprint>> ENCODED_BLUEPRINT =
            COMPONENTS.registerComponentType("encoded_blueprint",
                    builder -> builder.persistent(EncodedBlueprint.CODEC)
                            .networkSynchronized(EncodedBlueprint.STREAM_CODEC));

    /** The Network Receiver a Network Card is linked to. */
    public static final Supplier<DataComponentType<GlobalPos>> LINKED_RECEIVER = COMPONENTS.registerComponentType(
            "linked_receiver",
            builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    /**
     * Where the Nexus a Nexus Terminal reaches the network of stood when last
     * seen; a terminal bound before networks had ids knows nothing more.
     */
    public static final Supplier<DataComponentType<GlobalPos>> BOUND_NEXUS = COMPONENTS.registerComponentType(
            "bound_nexus",
            builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    /** The id of the network a Nexus Terminal reaches, wherever its Nexus stands. */
    public static final Supplier<DataComponentType<UUID>> BOUND_NETWORK = COMPONENTS.registerComponentType(
            "bound_network",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /** The id of the network of a broken Nexus, which it keeps when placed again. */
    public static final Supplier<DataComponentType<UUID>> NETWORK_ID = COMPONENTS.registerComponentType(
            "network_id",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /** The color of the network of a broken Nexus, which it keeps when placed again. */
    public static final Supplier<DataComponentType<NetworkColor>> NETWORK_COLOR = COMPONENTS.registerComponentType(
            "network_color",
            builder -> builder.persistent(Codec.intRange(0, NetworkColor.MAX_RGB)
                            .xmap(NetworkColor::new, NetworkColor::rgb))
                    .networkSynchronized(ByteBufCodecs.INT.map(NetworkColor::new, NetworkColor::rgb)));

    /** What a Nexus Terminal works as: a terminal, a crafting terminal or a blueprint terminal. */
    public static final Supplier<DataComponentType<TerminalKind>> TERMINAL_MODE = COMPONENTS.registerComponentType(
            "terminal_mode",
            builder -> builder.persistent(TerminalKind.CODEC)
                    .networkSynchronized(NeoForgeStreamCodecs.enumCodec(TerminalKind.class)));

    /** How the player of a Nexus Terminal likes its list sorted and sized. */
    public static final Supplier<DataComponentType<TerminalSettings>> TERMINAL_SETTINGS =
            COMPONENTS.registerComponentType("terminal_settings",
                    builder -> builder.persistent(TerminalSettings.CODEC)
                            .networkSynchronized(TerminalSettings.STREAM_CODEC));

    private NexusDataComponents() {
    }
}
