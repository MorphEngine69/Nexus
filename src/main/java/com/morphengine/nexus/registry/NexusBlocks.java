package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.CraftingMonitorBlock;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.ExternalVaultBlock;
import com.morphengine.nexus.block.GeneratorBlock;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.block.WirelessKind;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.machine.MachineTier;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.transfer.TransferKind;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NexusBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nexus.MOD_ID);

    public static final DeferredBlock<NexusBlock> NEXUS = BLOCKS.registerBlock(
            "nexus",
            NexusBlock::new,
            device());

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

    /** The generators of every kind. */
    public static final Map<GeneratorKind, DeferredBlock<GeneratorBlock>> GENERATORS = registerGenerators();

    /** The machines of every kind, the lowest tier first. */
    public static final Map<MachineKind, List<DeferredBlock<MachineBlock>>> MACHINES = registerMachines();

    public static final DeferredBlock<StorageVaultBlock> STORAGE_VAULT = BLOCKS.registerBlock(
            "storage_vault",
            StorageVaultBlock::new,
            device());

    public static final DeferredBlock<TerminalBlock> TERMINAL = BLOCKS.registerBlock(
            "terminal",
            properties -> new TerminalBlock(TerminalKind.TERMINAL, properties),
            device());

    public static final DeferredBlock<TerminalBlock> CRAFTING_TERMINAL = BLOCKS.registerBlock(
            "crafting_terminal",
            properties -> new TerminalBlock(TerminalKind.CRAFTING_TERMINAL, properties),
            device());

    public static final DeferredBlock<TransferDeviceBlock> PULLER = BLOCKS.registerBlock(
            "puller",
            properties -> new TransferDeviceBlock(TransferKind.PULLER, properties),
            device());

    public static final DeferredBlock<TransferDeviceBlock> PUSHER = BLOCKS.registerBlock(
            "pusher",
            properties -> new TransferDeviceBlock(TransferKind.PUSHER, properties),
            device());

    public static final DeferredBlock<TransferDeviceBlock> PLACER = BLOCKS.registerBlock(
            "placer",
            properties -> new TransferDeviceBlock(TransferKind.PLACER, properties),
            device());

    public static final DeferredBlock<TransferDeviceBlock> REMOVER = BLOCKS.registerBlock(
            "remover",
            properties -> new TransferDeviceBlock(TransferKind.REMOVER, properties),
            device());

    public static final DeferredBlock<ExternalVaultBlock> EXTERNAL_VAULT = BLOCKS.registerBlock(
            "external_vault",
            ExternalVaultBlock::new,
            device());

    public static final DeferredBlock<AssemblerBlock> ASSEMBLER = BLOCKS.registerBlock(
            "assembler",
            AssemblerBlock::new,
            device());

    public static final DeferredBlock<CraftingMonitorBlock> CRAFTING_MONITOR = BLOCKS.registerBlock(
            "crafting_monitor",
            CraftingMonitorBlock::new,
            device());

    public static final DeferredBlock<TerminalBlock> BLUEPRINT_TERMINAL = BLOCKS.registerBlock(
            "blueprint_terminal",
            properties -> new TerminalBlock(TerminalKind.BLUEPRINT_TERMINAL, properties),
            device());

    public static final DeferredBlock<WirelessBlock> NETWORK_TRANSMITTER = BLOCKS.registerBlock(
            "network_transmitter",
            properties -> new WirelessBlock(WirelessKind.TRANSMITTER, properties),
            device());

    public static final DeferredBlock<WirelessBlock> NETWORK_RECEIVER = BLOCKS.registerBlock(
            "network_receiver",
            properties -> new WirelessBlock(WirelessKind.RECEIVER, properties),
            device());

    public static final DeferredBlock<WirelessBlock> NEXUS_LINK = BLOCKS.registerBlock(
            "nexus_link",
            properties -> new WirelessBlock(WirelessKind.LINK, properties),
            device());

    public static final Map<DyeColor, DeferredBlock<CableBlock>> CABLES = registerCables();

    private static final float DEVICE_HARDNESS = 3.5F;
    private static final float DEVICE_BLAST_RESISTANCE = 6.0F;
    private static final float CABLE_HARDNESS = 0.35F;
    private static final int ACTIVE_MACHINE_LIGHT = 9;

    private NexusBlocks() {
    }

    /**
     * The default blue cable is plain {@code cable}; every other color is {@code <color>_cable}.
     */
    public static String cableName(final DyeColor color) {
        return color == CableBlock.DEFAULT_COLOR ? "cable" : color.getSerializedName() + "_cable";
    }

    private static DeferredBlock<EnergyCellBlock> energyCell(final String name, final EnergyCellTier tier) {
        return BLOCKS.registerBlock(name, properties -> new EnergyCellBlock(tier, properties), device());
    }

    /**
     * @return the blocks of {@code kind}, one for each tier, the lowest first
     */
    public static List<DeferredBlock<MachineBlock>> machineTiers(final MachineKind kind) {
        return MACHINES.get(kind);
    }

    private static Map<MachineKind, List<DeferredBlock<MachineBlock>>> registerMachines() {
        final List<String> tierNames = List.of("basic", "advanced", "superior", "quantum");
        final Map<MachineKind, List<DeferredBlock<MachineBlock>>> machines = new EnumMap<>(MachineKind.class);
        for (MachineKind kind : MachineKind.values()) {
            final List<DeferredBlock<MachineBlock>> tiers = new ArrayList<>();
            for (int rank = 1; rank <= tierNames.size(); rank++) {
                final MachineTier tier = MachineTier.ofRank(rank);
                tiers.add(BLOCKS.registerBlock(tierNames.get(rank - 1) + "_" + kind.id(),
                        properties -> new MachineBlock(kind, tier, properties), machine()));
            }
            machines.put(kind, List.copyOf(tiers));
        }
        return Collections.unmodifiableMap(machines);
    }

    private static Map<DyeColor, DeferredBlock<CableBlock>> registerCables() {
        final Map<DyeColor, DeferredBlock<CableBlock>> cables = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            cables.put(color, BLOCKS.registerBlock(
                    cableName(color),
                    properties -> new CableBlock(color, properties),
                    BlockBehaviour.Properties.of().strength(CABLE_HARDNESS).sound(SoundType.METAL)
                            .pushReaction(PushReaction.BLOCK)));
        }
        return Collections.unmodifiableMap(cables);
    }

    private static BlockBehaviour.Properties device() {
        return BlockBehaviour.Properties.of()
                .strength(DEVICE_HARDNESS, DEVICE_BLAST_RESISTANCE).sound(SoundType.METAL).noOcclusion();
    }

    private static BlockBehaviour.Properties machine() {
        return device().lightLevel(
                state -> state.getValue(MachineBlock.PHASE) == MachinePhase.ACTIVE ? ACTIVE_MACHINE_LIGHT : 0);
    }

    /**
     * The light is worked out from the kind, not from the block: the block does not exist yet when its states are made.
     */
    private static BlockBehaviour.Properties generator(final GeneratorKind kind) {
        return device().lightLevel(
                state -> state.getValue(GeneratorBlock.PHASE) == MachinePhase.ACTIVE ? kind.workLight() : 0);
    }

    private static Map<GeneratorKind, DeferredBlock<GeneratorBlock>> registerGenerators() {
        final Map<GeneratorKind, DeferredBlock<GeneratorBlock>> generators = new EnumMap<>(GeneratorKind.class);
        for (GeneratorKind kind : GeneratorKind.values()) {
            generators.put(kind, BLOCKS.registerBlock(kind.id(),
                    properties -> new GeneratorBlock(kind, properties), generator(kind)));
        }
        return Collections.unmodifiableMap(generators);
    }
}
