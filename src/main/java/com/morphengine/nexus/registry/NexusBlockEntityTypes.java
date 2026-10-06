package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.block.entity.CraftingMonitorBlockEntity;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.block.entity.NetworkReceiverBlockEntity;
import com.morphengine.nexus.block.entity.NetworkTransmitterBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class NexusBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nexus.MOD_ID);

    public static final Supplier<BlockEntityType<NexusBlockEntity>> NEXUS = BLOCK_ENTITY_TYPES.register(
            "nexus",
            () -> new BlockEntityType<>(NexusBlockEntity::new, Set.of(NexusBlocks.NEXUS.get())));

    public static final Supplier<BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELL = BLOCK_ENTITY_TYPES.register(
            "energy_cell",
            () -> new BlockEntityType<>(EnergyCellBlockEntity::new, NexusBlocks.ENERGY_CELLS.stream()
                    .<Block>map(DeferredBlock::get).collect(Collectors.toUnmodifiableSet())));

    /** Every machine of every kind and tier. */
    public static final Supplier<BlockEntityType<MachineBlockEntity>> MACHINE = BLOCK_ENTITY_TYPES.register(
            "machine",
            () -> new BlockEntityType<>(MachineBlockEntity::new, NexusBlocks.MACHINES.values().stream()
                    .flatMap(List::stream).<Block>map(DeferredBlock::get)
                    .collect(Collectors.toUnmodifiableSet())));

    /**
     * Every generator of every kind. Its id is that of the first generator, so that the Coal Generators in saved
     * worlds keep their fuel and energy.
     */
    public static final Supplier<BlockEntityType<GeneratorBlockEntity>> GENERATOR = BLOCK_ENTITY_TYPES.register(
            "coal_generator",
            () -> new BlockEntityType<>(GeneratorBlockEntity::new, NexusBlocks.GENERATORS.values().stream()
                    .<Block>map(DeferredBlock::get).collect(Collectors.toUnmodifiableSet())));

    public static final Supplier<BlockEntityType<StorageVaultBlockEntity>> STORAGE_VAULT =
            BLOCK_ENTITY_TYPES.register("storage_vault", () -> new BlockEntityType<>(
                    StorageVaultBlockEntity::new, Set.of(NexusBlocks.STORAGE_VAULT.get())));

    /** Every kind of terminal; the block tells which. */
    public static final Supplier<BlockEntityType<TerminalBlockEntity>> TERMINAL =
            BLOCK_ENTITY_TYPES.register("terminal", () -> new BlockEntityType<>(
                    TerminalBlockEntity::new, Set.of(NexusBlocks.TERMINAL.get(), NexusBlocks.CRAFTING_TERMINAL.get(),
                    NexusBlocks.BLUEPRINT_TERMINAL.get())));

    /** Every attached device, Puller, Pusher, Placer and Remover; the block tells which. */
    public static final Supplier<BlockEntityType<TransferDeviceBlockEntity>> TRANSFER_DEVICE =
            BLOCK_ENTITY_TYPES.register("transfer_device", () -> new BlockEntityType<>(
                    TransferDeviceBlockEntity::new, Set.of(NexusBlocks.PULLER.get(), NexusBlocks.PUSHER.get(),
                            NexusBlocks.PLACER.get(), NexusBlocks.REMOVER.get())));

    public static final Supplier<BlockEntityType<AssemblerBlockEntity>> ASSEMBLER =
            BLOCK_ENTITY_TYPES.register("assembler", () -> new BlockEntityType<>(
                    AssemblerBlockEntity::new, Set.of(NexusBlocks.ASSEMBLER.get())));

    public static final Supplier<BlockEntityType<CraftingMonitorBlockEntity>> CRAFTING_MONITOR =
            BLOCK_ENTITY_TYPES.register("crafting_monitor", () -> new BlockEntityType<>(
                    CraftingMonitorBlockEntity::new, Set.of(NexusBlocks.CRAFTING_MONITOR.get())));

    public static final Supplier<BlockEntityType<NetworkTransmitterBlockEntity>> NETWORK_TRANSMITTER =
            BLOCK_ENTITY_TYPES.register("network_transmitter", () -> new BlockEntityType<>(
                    NetworkTransmitterBlockEntity::new, Set.of(NexusBlocks.NETWORK_TRANSMITTER.get())));

    public static final Supplier<BlockEntityType<NetworkReceiverBlockEntity>> NETWORK_RECEIVER =
            BLOCK_ENTITY_TYPES.register("network_receiver", () -> new BlockEntityType<>(
                    NetworkReceiverBlockEntity::new, Set.of(NexusBlocks.NETWORK_RECEIVER.get())));

    public static final Supplier<BlockEntityType<NexusLinkBlockEntity>> NEXUS_LINK =
            BLOCK_ENTITY_TYPES.register("nexus_link", () -> new BlockEntityType<>(
                    NexusLinkBlockEntity::new, Set.of(NexusBlocks.NEXUS_LINK.get())));

    private NexusBlockEntityTypes() {
    }
}
