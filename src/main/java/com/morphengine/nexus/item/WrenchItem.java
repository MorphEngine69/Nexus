package com.morphengine.nexus.item;

import com.morphengine.nexus.block.NetworkBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Wrench: a right click turns a block of a network, a right click while sneaking takes it down into the
 * inventory, and a left click sets its front to the side that was struck, see {@link WrenchLeftClick}.
 */
public final class WrenchItem extends Item {

    public WrenchItem(final Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null || !(context.getLevel().getBlockState(context.getClickedPos()).getBlock()
                instanceof NetworkBlock)) {
            return InteractionResult.PASS;
        }
        return player.isSecondaryUseActive()
                ? WrenchActions.dismantle(context.getLevel(), context.getClickedPos(), player)
                : WrenchActions.turn(context.getLevel(), context.getClickedPos(), player, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final List<Component> tooltip,
            final TooltipFlag flag) {
        final Consumer<Component> builder = tooltip::add;
        for (String line : new String[] {"turn", "front", "dismantle"}) {
            builder.accept(Component.translatable("tooltip.nexus.wrench." + line).withStyle(ChatFormatting.GRAY));
        }
    }
}
