package com.morphengine.nexus.registry;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.VaultCellItem;
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
                        output.accept(NexusItems.BASIC_ENERGY_CELL.get());
                        output.accept(NexusItems.COAL_GENERATOR.get());
                        output.accept(NexusItems.STORAGE_VAULT.get());
                        output.accept(NexusItems.TERMINAL.get());
                        output.accept(NexusItems.CRAFTING_TERMINAL.get());
                        output.accept(NexusItems.PULLER.get());
                        output.accept(NexusItems.PUSHER.get());
                        for (Map<CellTier, DeferredItem<VaultCellItem>> tiers : NexusItems.VAULT_CELLS.values()) {
                            for (DeferredItem<VaultCellItem> cell : tiers.values()) {
                                output.accept(cell.get());
                            }
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
