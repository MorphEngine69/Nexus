package com.morphengine.nexus.item;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.access.Secured;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.menu.AnalyserMenu;
import com.morphengine.nexus.menu.AnalyserView;
import com.morphengine.nexus.menu.AnalyserViews;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.function.Consumer;

/**
 * A Nexus Analyser: used on a device of a network, or on its Nexus, it opens a panel with what the block draws,
 * supplies and pays in fees for its operations. It opens only for a player who may open the block.
 */
public final class NexusAnalyserItem extends Item {

    public NexusAnalyserItem(final Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockEntity target = level.getBlockEntity(pos);
        if (level.isClientSide() || !(context.getPlayer() instanceof ServerPlayer player) || target == null) {
            return target == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        if (AnalyserViews.of(target, level.getGameTime()).kind() == AnalyserView.Kind.NOTHING) {
            player.displayClientMessage(Component.translatable("item.nexus.nexus_analyser.nothing"), true);
            return InteractionResult.FAIL;
        }
        if (target instanceof Secured secured && !NetworkAccess.permits(player, secured, Permission.OPEN)) {
            NetworkAccess.refuse(player, Permission.OPEN);
            return InteractionResult.FAIL;
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, opener) -> new AnalyserMenu(containerId, inventory, pos),
                getName(context.getItemInHand())), buffer -> buffer.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final List<Component> tooltip,
            final TooltipFlag flag) {
        final Consumer<Component> builder = tooltip::add;
        builder.accept(Component.translatable("tooltip.nexus.nexus_analyser").withStyle(ChatFormatting.GRAY));
    }
}
