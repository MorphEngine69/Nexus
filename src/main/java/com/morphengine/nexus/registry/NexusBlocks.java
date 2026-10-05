package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.CoalGeneratorBlock;
import com.morphengine.nexus.block.CraftingMonitorBlock;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.block.WirelessKind;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.transfer.TransferKind;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NexusBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nexus.MOD_ID);

    public static final DeferredBlock<NexusBlock> NEXUS = BLOCKS.registerBlock(
            "nexus",
            NexusBlock::new,
            NexusBlocks::device);

    public static final DeferredBlock<EnergyCellBlock> BASIC_ENERGY_CELL =
            energyCell("basic_energy_cell", EnergyCellTier.BASIC);

    public static final DeferredBlock<EnergyCellBlock> ADVANCED_ENERGY_CELL =
            energyCell("advanced_energy_cell", EnergyCellTier.ADVANCED);

    public static final DeferredBlock<EnergyCellBlock> SUPERIOR_ENERGY_CELL =
            energyCell("superior_energy_cell", EnergyCellTier.SUPERIOR);

    public static final DeferredBlock<EnergyCellBlock> QUANTUM_ENERGY_CELL =
            energyCell("quantum_energy_cell", EnergyCellTier.QUANTUM);

    /** Every Energy Cell, the smallest first. */
    public static final List<DeferredBlock<EnergyCellBlock>> ENERGY_CELLS = List.of(
            BASIC_ENERGY_CELL, ADVANCED_ENERGY_CELL, SUPERIOR_ENERGY_CELL, QUANTUM_ENERGY_CELL);

    public static final DeferredBlock<CoalGeneratorBlock> COAL_GENERATOR = BLOCKS.registerBlock(
            "coal_generator",
            CoalGeneratorBlock::new,
            NexusBlocks::generator);

    public static final DeferredBlock<StorageVaultBlock> STORAGE_VAULT = BLOCKS.registerBlock(
            "storage_vault",
            StorageVaultBlock::new,
            NexusBlocks::device);

    public static final DeferredBlock<TerminalBlock> TERMINAL = BLOCKS.registerBlock(
            "terminal",
            properties -> new TerminalBlock(TerminalKind.TERMINAL, properties),
            NexusBlocks::device);

    public static final DeferredBlock<TerminalBlock> CRAFTING_TERMINAL = BLOCKS.registerBlock(
            "crafting_terminal",
            properties -> new TerminalBlock(TerminalKind.CRAFTING_TERMINAL, properties),
            NexusBlocks::device);

    public static final DeferredBlock<TransferDeviceBlock> PULLER = BLOCKS.registerBlock(
            "puller",
            properties -> new TransferDeviceBlock(TransferKind.PULLER, properties),
            NexusBlocks::device);

    public static final DeferredBlock<TransferDeviceBlock> PUSHER = BLOCKS.registerBlock(
            "pusher",
            properties -> new TransferDeviceBlock(TransferKind.PUSHER, properties),
            NexusBlocks::device);

    public static final DeferredBlock<TransferDeviceBlock> PLACER = BLOCKS.registerBlock(
            "placer",
            properties -> new TransferDeviceBlock(TransferKind.PLACER, properties),
            NexusBlocks::device);

    public static final DeferredBlock<TransferDeviceBlock> REMOVER = BLOCKS.registerBlock(
            "remover",
            properties -> new TransferDeviceBlock(TransferKind.REMOVER, properties),
            NexusBlocks::device);

    public static final DeferredBlock<AssemblerBlock> ASSEMBLER = BLOCKS.registerBlock(
            "assembler",
            AssemblerBlock::new,
            NexusBlocks::device);

    public static final DeferredBlock<CraftingMonitorBlock> CRAFTING_MONITOR = BLOCKS.registerBlock(
            "crafting_monitor",
            CraftingMonitorBlock::new,
            NexusBlocks::device);

    public static final DeferredBlock<TerminalBlock> BLUEPRINT_TERMINAL = BLOCKS.registerBlock(
            "blueprint_terminal",
            properties -> new TerminalBlock(TerminalKind.BLUEPRINT_TERMINAL, properties),
            NexusBlocks::device);

    public static final DeferredBlock<WirelessBlock> NETWORK_TRANSMITTER = BLOCKS.registerBlock(
            "network_transmitter",
            properties -> new WirelessBlock(WirelessKind.TRANSMITTER, properties),
            NexusBlocks::device);

    public static final DeferredBlock<WirelessBlock> NETWORK_RECEIVER = BLOCKS.registerBlock(
            "network_receiver",
            properties -> new WirelessBlock(WirelessKind.RECEIVER, properties),
            NexusBlocks::device);

    public static final DeferredBlock<WirelessBlock> NEXUS_LINK = BLOCKS.registerBlock(
            "nexus_link",
            properties -> new WirelessBlock(WirelessKind.LINK, properties),
            NexusBlocks::device);

    public static final Map<DyeColor, DeferredBlock<CableBlock>> CABLES = registerCables();

    private static final float DEVICE_HARDNESS = 3.5F;
    private static final float DEVICE_BLAST_RESISTANCE = 6.0F;
    private static final float CABLE_HARDNESS = 0.35F;
    private static final int LIT_GENERATOR_LIGHT = 13;

    private NexusBlocks() {
    }

    /**
     * The default blue cable is plain {@code cable}; every other color is {@code <color>_cable}.
     */
    public static String cableName(final DyeColor color) {
        return color == CableBlock.DEFAULT_COLOR ? "cable" : color.getSerializedName() + "_cable";
    }

    private static DeferredBlock<EnergyCellBlock> energyCell(final String name, final EnergyCellTier tier) {
        return BLOCKS.registerBlock(name, properties -> new EnergyCellBlock(tier, properties), NexusBlocks::device);
    }

    private static Map<DyeColor, DeferredBlock<CableBlock>> registerCables() {
        final Map<DyeColor, DeferredBlock<CableBlock>> cables = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            cables.put(color, BLOCKS.registerBlock(
                    cableName(color),
                    properties -> new CableBlock(color, properties),
                    properties -> properties.strength(CABLE_HARDNESS).sound(SoundType.METAL)
                            .pushReaction(PushReaction.BLOCK)));
        }
        return Collections.unmodifiableMap(cables);
    }

    private static BlockBehaviour.Properties device(final BlockBehaviour.Properties properties) {
        return properties.strength(DEVICE_HARDNESS, DEVICE_BLAST_RESISTANCE).sound(SoundType.METAL).noOcclusion();
    }

    private static BlockBehaviour.Properties generator(final BlockBehaviour.Properties properties) {
        return device(properties).lightLevel(state -> state.getValue(CoalGeneratorBlock.LIT) ? LIT_GENERATOR_LIGHT : 0);
    }
}
