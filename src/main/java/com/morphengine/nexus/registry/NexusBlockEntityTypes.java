package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.block.entity.CraftingMonitorBlockEntity;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class NexusBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nexus.MOD_ID);

    public static final Supplier<BlockEntityType<NexusBlockEntity>> NEXUS = BLOCK_ENTITY_TYPES.register(
            "nexus",
            () -> typeOf(NexusBlockEntity::new, Set.of(NexusBlocks.NEXUS.get())));

    public static final Supplier<BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELL = BLOCK_ENTITY_TYPES.register(
            "energy_cell",
            () -> typeOf(EnergyCellBlockEntity::new, NexusBlocks.ENERGY_CELLS.stream()
                    .<Block>map(DeferredBlock::get).collect(Collectors.toUnmodifiableSet())));

    /** Every machine of every kind and tier. */
    public static final Supplier<BlockEntityType<MachineBlockEntity>> MACHINE = BLOCK_ENTITY_TYPES.register(
            "machine",
            () -> typeOf(MachineBlockEntity::new, NexusBlocks.MACHINES.values().stream()
                    .flatMap(List::stream).<Block>map(DeferredBlock::get)
                    .collect(Collectors.toUnmodifiableSet())));

    /**
     * Every generator of every kind. Its id is that of the first generator, so that the Coal Generators in saved
     * worlds keep their fuel and energy.
     */
    public static final Supplier<BlockEntityType<GeneratorBlockEntity>> GENERATOR = BLOCK_ENTITY_TYPES.register(
            "coal_generator",
            () -> typeOf(GeneratorBlockEntity::new, NexusBlocks.GENERATORS.values().stream()
                    .<Block>map(DeferredBlock::get).collect(Collectors.toUnmodifiableSet())));

    public static final Supplier<BlockEntityType<StorageVaultBlockEntity>> STORAGE_VAULT =
            BLOCK_ENTITY_TYPES.register("storage_vault", () -> typeOf(
                    StorageVaultBlockEntity::new, Set.of(NexusBlocks.STORAGE_VAULT.get())));

    /** Every kind of terminal; the block tells which. */
    public static final Supplier<BlockEntityType<TerminalBlockEntity>> TERMINAL =
            BLOCK_ENTITY_TYPES.register("terminal", () -> typeOf(
                    TerminalBlockEntity::new, Set.of(NexusBlocks.TERMINAL.get(), NexusBlocks.CRAFTING_TERMINAL.get(),
                    NexusBlocks.BLUEPRINT_TERMINAL.get())));

    /** Every attached device, Puller, Pusher, Placer and Remover; the block tells which. */
    public static final Supplier<BlockEntityType<TransferDeviceBlockEntity>> TRANSFER_DEVICE =
            BLOCK_ENTITY_TYPES.register("transfer_device", () -> typeOf(
                    TransferDeviceBlockEntity::new, Set.of(NexusBlocks.PULLER.get(), NexusBlocks.PUSHER.get(),
                            NexusBlocks.PLACER.get(), NexusBlocks.REMOVER.get())));

    public static final Supplier<BlockEntityType<ExternalVaultBlockEntity>> EXTERNAL_VAULT =
            BLOCK_ENTITY_TYPES.register("external_vault", () -> typeOf(
                    ExternalVaultBlockEntity::new, Set.of(NexusBlocks.EXTERNAL_VAULT.get())));

    public static final Supplier<BlockEntityType<AssemblerBlockEntity>> ASSEMBLER =
            BLOCK_ENTITY_TYPES.register("assembler", () -> typeOf(
                    AssemblerBlockEntity::new, Set.of(NexusBlocks.ASSEMBLER.get())));

    public static final Supplier<BlockEntityType<CraftingMonitorBlockEntity>> CRAFTING_MONITOR =
            BLOCK_ENTITY_TYPES.register("crafting_monitor", () -> typeOf(
                    CraftingMonitorBlockEntity::new, Set.of(NexusBlocks.CRAFTING_MONITOR.get())));

    public static final Supplier<BlockEntityType<NetworkTransmitterBlockEntity>> NETWORK_TRANSMITTER =
            BLOCK_ENTITY_TYPES.register("network_transmitter", () -> typeOf(
                    NetworkTransmitterBlockEntity::new, Set.of(NexusBlocks.NETWORK_TRANSMITTER.get())));

    public static final Supplier<BlockEntityType<NetworkReceiverBlockEntity>> NETWORK_RECEIVER =
            BLOCK_ENTITY_TYPES.register("network_receiver", () -> typeOf(
                    NetworkReceiverBlockEntity::new, Set.of(NexusBlocks.NETWORK_RECEIVER.get())));

    public static final Supplier<BlockEntityType<NexusLinkBlockEntity>> NEXUS_LINK =
            BLOCK_ENTITY_TYPES.register("nexus_link", () -> typeOf(
                    NexusLinkBlockEntity::new, Set.of(NexusBlocks.NEXUS_LINK.get())));

    private NexusBlockEntityTypes() {
    }

    private static <T extends BlockEntity> BlockEntityType<T> typeOf(
            final BlockEntityType.BlockEntitySupplier<T> factory, final Collection<? extends Block> blocks) {
        return BlockEntityType.Builder.of(factory, blocks.toArray(new Block[0])).build(null);
    }
}
