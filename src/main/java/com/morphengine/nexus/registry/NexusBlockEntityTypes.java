package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.entity.CoalGeneratorBlockEntity;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

public final class NexusBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nexus.MOD_ID);

    public static final Supplier<BlockEntityType<NexusBlockEntity>> NEXUS = BLOCK_ENTITY_TYPES.register(
            "nexus",
            () -> new BlockEntityType<>(NexusBlockEntity::new, Set.of(NexusBlocks.NEXUS.get())));

    public static final Supplier<BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELL = BLOCK_ENTITY_TYPES.register(
            "energy_cell",
            () -> new BlockEntityType<>(EnergyCellBlockEntity::new, Set.of(NexusBlocks.BASIC_ENERGY_CELL.get())));

    public static final Supplier<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR =
            BLOCK_ENTITY_TYPES.register("coal_generator", () -> new BlockEntityType<>(
                    CoalGeneratorBlockEntity::new, Set.of(NexusBlocks.COAL_GENERATOR.get())));

    public static final Supplier<BlockEntityType<StorageVaultBlockEntity>> STORAGE_VAULT =
            BLOCK_ENTITY_TYPES.register("storage_vault", () -> new BlockEntityType<>(
                    StorageVaultBlockEntity::new, Set.of(NexusBlocks.STORAGE_VAULT.get())));

    /** Both the Terminal and the Crafting Terminal; the block tells which. */
    public static final Supplier<BlockEntityType<TerminalBlockEntity>> TERMINAL =
            BLOCK_ENTITY_TYPES.register("terminal", () -> new BlockEntityType<>(
                    TerminalBlockEntity::new, Set.of(NexusBlocks.TERMINAL.get(), NexusBlocks.CRAFTING_TERMINAL.get())));

    private NexusBlockEntityTypes() {
    }
}
