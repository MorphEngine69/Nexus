package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.CoalGeneratorBlock;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.terminal.TerminalKind;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class NexusBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nexus.MOD_ID);

    public static final DeferredBlock<NexusBlock> NEXUS = BLOCKS.registerBlock(
            "nexus",
            NexusBlock::new,
            NexusBlocks::device);

    public static final DeferredBlock<EnergyCellBlock> BASIC_ENERGY_CELL = BLOCKS.registerBlock(
            "basic_energy_cell",
            properties -> new EnergyCellBlock(EnergyCellTier.BASIC, properties),
            NexusBlocks::device);

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

    private static Map<DyeColor, DeferredBlock<CableBlock>> registerCables() {
        final Map<DyeColor, DeferredBlock<CableBlock>> cables = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            cables.put(color, BLOCKS.registerBlock(
                    cableName(color),
                    properties -> new CableBlock(color, properties),
                    properties -> properties.strength(CABLE_HARDNESS).sound(SoundType.METAL)));
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
