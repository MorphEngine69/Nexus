package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.DeviceBlockItem;
import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.item.TierUpgradeItem;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.metal.MetalKind;
import com.morphengine.nexus.metal.MetalPart;
import com.morphengine.nexus.metal.ToolPart;
import com.morphengine.nexus.metal.VanillaMetal;
import com.morphengine.nexus.registry.NexusMetals.MetalSet;
import com.morphengine.nexus.upgrade.UpgradeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The creative tab of the mod. Its items go in sections, each one a kind of thing, in the order of the network, then
 * what the network is made of and with: the Nexus, terminals, storage, power, machines, automation, upgrades,
 * components, metals and the rest of the materials, tools and armor.
 */
public final class NexusCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nexus.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nexus"))
                    .icon(() -> new ItemStack(NexusItems.NEXUS.get()))
                    .displayItems((parameters, output) -> {
                        network(output);
                        terminals(output);
                        storage(output);
                        power(output);
                        machines(output);
                        automation(output);
                        upgrades(output);
                        components(output);
                        metals(output);
                        otherMaterials(output);
                        equipment(output);
                    })
                    .build());

    private static final List<ArmorItem.Type> ARMOR_ORDER =
            List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);

    private NexusCreativeTabs() {
    }

    private static void network(final CreativeModeTab.Output output) {
        output.accept(NexusItems.NEXUS.get());
        output.accept(NexusItems.CABLES.get(CableBlock.DEFAULT_COLOR).get());
        for (DyeColor color : DyeColor.values()) {
            if (color != CableBlock.DEFAULT_COLOR) {
                output.accept(NexusItems.CABLES.get(color).get());
            }
        }
        output.accept(NexusItems.NEXUS_LINK.get());
        output.accept(NexusItems.NETWORK_TRANSMITTER.get());
        output.accept(NexusItems.NETWORK_RECEIVER.get());
        output.accept(NexusItems.NETWORK_CARD.get());
        output.accept(NexusItems.WRENCH.get());
        output.accept(NexusItems.NEXUS_ANALYSER.get());
    }

    private static ItemStack chargedTerminal() {
        final ItemStack terminal = new ItemStack(NexusItems.NEXUS_TERMINAL.get());
        terminal.set(NexusDataComponents.TERMINAL_CHARGE.get(), NexusTerminalItem.capacity());
        return terminal;
    }

    private static void terminals(final CreativeModeTab.Output output) {
        output.accept(NexusItems.TERMINAL.get());
        output.accept(NexusItems.CRAFTING_TERMINAL.get());
        output.accept(NexusItems.BLUEPRINT_TERMINAL.get());
        output.accept(NexusItems.NEXUS_TERMINAL.get());
        output.accept(chargedTerminal());
        output.accept(NexusItems.CRAFTING_MONITOR.get());
        output.accept(NexusItems.BLUEPRINT.get());
    }

    private static void storage(final CreativeModeTab.Output output) {
        output.accept(NexusItems.STORAGE_VAULT.get());
        output.accept(NexusItems.EXTERNAL_VAULT.get());
        for (Map<CellTier, DeferredItem<VaultCellItem>> tiers : NexusItems.VAULT_CELLS.values()) {
            for (DeferredItem<VaultCellItem> cell : tiers.values()) {
                output.accept(cell.get());
            }
        }
        output.accept(NexusMaterials.CELL_HOUSING.get());
        for (CellTier tier : CellTier.values()) {
            output.accept(NexusMaterials.CELL_PARTS.get(tier).get());
        }
    }

    private static void power(final CreativeModeTab.Output output) {
        for (DeferredItem<DeviceBlockItem> cell : NexusItems.ENERGY_CELLS) {
            output.accept(cell.get());
        }
        for (DeferredItem<DeviceBlockItem> generator : NexusItems.GENERATORS.values()) {
            output.accept(generator.get());
        }
    }

    private static void machines(final CreativeModeTab.Output output) {
        NexusItems.MACHINES.values().forEach(tiers -> tiers.forEach(machine -> output.accept(machine.get())));
    }

    private static void automation(final CreativeModeTab.Output output) {
        output.accept(NexusItems.PULLER.get());
        output.accept(NexusItems.PUSHER.get());
        output.accept(NexusItems.PLACER.get());
        output.accept(NexusItems.REMOVER.get());
        output.accept(NexusItems.ASSEMBLER.get());
    }

    private static void upgrades(final CreativeModeTab.Output output) {
        for (DeferredItem<UpgradeItem> upgrade : NexusItems.UPGRADES) {
            output.accept(upgrade.get());
        }
        for (DeferredItem<TierUpgradeItem> upgrade : NexusItems.TIER_UPGRADES) {
            output.accept(upgrade.get());
        }
    }

    private static void components(final CreativeModeTab.Output output) {
        output.accept(NexusMaterials.CORE.get());
        output.accept(NexusMaterials.UPGRADE_BLANK.get());
        output.accept(NexusMaterials.MACHINE_CASING.get());
        output.accept(NexusMaterials.BATTERY.get());
        output.accept(NexusMaterials.SCREEN.get());
        output.accept(NexusMaterials.ANTENNA.get());
        output.accept(NexusMaterials.NEXUS_CRYSTAL.get());
        output.accept(NexusMaterials.NEXUS_ORE_ITEM.get());
        output.accept(NexusMaterials.DEEPSLATE_NEXUS_ORE_ITEM.get());
    }

    private static void metals(final CreativeModeTab.Output output) {
        for (MetalKind metal : MetalKind.ALL) {
            final MetalSet set = NexusMetals.of(metal);
            set.oreItems().forEach(ore -> output.accept(ore.get()));
            for (MetalPart part : MetalPart.values()) {
                output.accept(set.part(part).get());
            }
            output.accept(set.storageBlockItem().get());
        }
    }

    private static void otherMaterials(final CreativeModeTab.Output output) {
        output.accept(NexusMaterials.VOLTSTEEL_INGOT.get());
        output.accept(NexusMaterials.LUMEN_INGOT.get());
        output.accept(NexusMaterials.AETHER_INGOT.get());
        for (VanillaMetal metal : VanillaMetal.values()) {
            output.accept(NexusMaterials.VANILLA_DUSTS.get(metal).get());
            output.accept(NexusMaterials.VANILLA_PLATES.get(metal).get());
        }
        output.accept(NexusMaterials.POLYMER.get());
        output.accept(NexusMaterials.BIOMASS.get());
        output.accept(NexusFluids.BIOFUEL_BUCKET.get());
    }

    private static void equipment(final CreativeModeTab.Output output) {
        for (MetalKind metal : MetalKind.ALL) {
            final MetalSet set = NexusMetals.of(metal);
            for (ToolPart tool : ToolPart.values()) {
                output.accept(set.tools().get(tool).get());
            }
            for (ArmorItem.Type piece : ARMOR_ORDER) {
                output.accept(set.armor().get(piece).get());
            }
        }
    }
}
