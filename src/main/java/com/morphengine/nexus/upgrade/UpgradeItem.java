package com.morphengine.nexus.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * An upgrade: goes into the upgrade slots of a device that takes its kind. Its
 * tooltip tells what it does, and whether it is still in development. Meant for
 * extension by an upgrade that also does something in the hand; a subclass
 * keeps the tooltip by calling {@code super.appendHoverText}.
 */
public class UpgradeItem extends Item {

    private final DeferredHolder<NexusUpgradeType, NexusUpgradeType> type;

    public UpgradeItem(
            final DeferredHolder<NexusUpgradeType, NexusUpgradeType> type, final Item.Properties properties) {
        super(properties);
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public final NexusUpgradeType type() {
        return type.get();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.nexus.upgrade." + type.getId().getPath())
                .withStyle(ChatFormatting.GRAY));
        if (type().isInDevelopment()) {
            builder.accept(Component.translatable("tooltip.nexus.upgrade.in_development")
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        }
    }
}
