package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.DeviceBlockItem;
import com.morphengine.nexus.item.TierUpgradeItem;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.upgrade.UpgradeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.function.Supplier;

public final class NexusCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nexus.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nexus"))
                    .icon(() -> new ItemStack(NexusItems.NEXUS.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(NexusItems.NEXUS.get());
                        for (DeferredItem<DeviceBlockItem> cell : NexusItems.ENERGY_CELLS) {
                            output.accept(cell.get());
                        }
                        output.accept(NexusItems.COAL_GENERATOR.get());
                        NexusItems.MACHINES.values().forEach(tiers -> tiers.forEach(
                                machine -> output.accept(machine.get())));
                        output.accept(NexusItems.STORAGE_VAULT.get());
                        output.accept(NexusItems.TERMINAL.get());
                        output.accept(NexusItems.CRAFTING_TERMINAL.get());
                        output.accept(NexusItems.PULLER.get());
                        output.accept(NexusItems.PUSHER.get());
                        output.accept(NexusItems.PLACER.get());
                        output.accept(NexusItems.REMOVER.get());
                        output.accept(NexusItems.BLUEPRINT_TERMINAL.get());
                        output.accept(NexusItems.ASSEMBLER.get());
                        output.accept(NexusItems.CRAFTING_MONITOR.get());
                        output.accept(NexusItems.BLUEPRINT.get());
                        output.accept(NexusItems.NETWORK_TRANSMITTER.get());
                        output.accept(NexusItems.NETWORK_RECEIVER.get());
                        output.accept(NexusItems.NETWORK_CARD.get());
                        output.accept(NexusItems.NEXUS_LINK.get());
                        output.accept(NexusItems.NEXUS_TERMINAL.get());
                        output.accept(NexusItems.WRENCH.get());
                        for (Map<CellTier, DeferredItem<VaultCellItem>> tiers : NexusItems.VAULT_CELLS.values()) {
                            for (DeferredItem<VaultCellItem> cell : tiers.values()) {
                                output.accept(cell.get());
                            }
                        }
                        for (DeferredItem<UpgradeItem> upgrade : NexusItems.UPGRADES) {
                            output.accept(upgrade.get());
                        }
                        for (DeferredItem<TierUpgradeItem> upgrade : NexusItems.TIER_UPGRADES) {
                            output.accept(upgrade.get());
                        }
                        output.accept(NexusItems.CABLES.get(CableBlock.DEFAULT_COLOR).get());
                        for (DyeColor color : DyeColor.values()) {
                            if (color != CableBlock.DEFAULT_COLOR) {
                                output.accept(NexusItems.CABLES.get(color).get());
                            }
                        }
                    })
                    .build());

    private NexusCreativeTabs() {
    }
}
